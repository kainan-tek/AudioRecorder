package com.example.audiorecorder.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.audiorecorder.model.WaveFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 录音器状态枚举
 */
enum class RecorderState {
    IDLE,      // 空闲状态
    RECORDING, // 录音中
    PAUSED,    // 暂停状态
    ERROR      // 错误状态
}

class AudioRecorder(private val context: Context) {
    companion object {
        private const val LOG_TAG = "AudioRecorder"
        private const val MIN_BUF_MULTIPLIER = 2
        private const val DEFAULT_SAMPLE_RATE = 48000
        private const val DEFAULT_CHANNELS = 1
        private const val DEFAULT_BITS_PER_SAMPLE = 16
        
        // 自定义多通道配置常量
        const val CHANNEL_IN_10 = 4092 // 0xFFC
        const val CHANNEL_IN_12 = 16380 // 0x3FFC
        const val CHANNEL_IN_14 = 65532 // 0xFFFC
        const val CHANNEL_IN_16 = 262140 // 0x3FFFC
    }

    private var audioSource = MediaRecorder.AudioSource.MIC
    private var sampleRate = DEFAULT_SAMPLE_RATE
    private var channels = DEFAULT_CHANNELS
    private var bitsPerSample = DEFAULT_BITS_PER_SAMPLE
    private var minBufSize = 0
    private var totalBytesRead = 0
    private var audioRecordFile: String = "/data/record_48k_1ch_16bit.wav"

    private var audioRecord: AudioRecord? = null
    private var waveFile: WaveFile? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    
    private val _recorderState = MutableLiveData(RecorderState.IDLE)
    val recorderState: LiveData<RecorderState> = _recorderState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(): Boolean {
        if (isRecording.get()) {
            Log.d(LOG_TAG, "Already recording, ignoring start request")
            return false
        }

        try {
            Log.d(LOG_TAG, "Starting recording with: sampleRate=$sampleRate, channels=$channels, bitsPerSample=$bitsPerSample")
            val outputFilePath = audioRecordFile.ifEmpty { generateOutputFilePath() }
            Log.d(LOG_TAG, "Output file path: $outputFilePath")
            
            waveFile = WaveFile(outputFilePath)
            if (waveFile?.create(sampleRate, channels, bitsPerSample) != true) {
                val file = File(outputFilePath)
                val msg = if (file.exists()) "File exists but access is denied" else "File does not exist"
                Log.e(LOG_TAG, "Failed to create WAV file: $msg")
                _errorMessage.postValue(msg)
                return false
            }
            Log.d(LOG_TAG, "WAV file created successfully")

            val channelConfig = when (channels) {
                1 -> AudioFormat.CHANNEL_IN_MONO
                2 -> AudioFormat.CHANNEL_IN_STEREO
                10 -> CHANNEL_IN_10
                12 -> CHANNEL_IN_12
                14 -> CHANNEL_IN_14
                16 -> CHANNEL_IN_16
                else -> {
                    Log.e(LOG_TAG, "Unsupported channel count: $channels")
                    _errorMessage.postValue("Unsupported channel count: $channels")
                    return false
                }
            }
                
            val audioFormat = when (bitsPerSample) {
                8 -> AudioFormat.ENCODING_PCM_8BIT
                16 -> AudioFormat.ENCODING_PCM_16BIT
                24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
                32 -> AudioFormat.ENCODING_PCM_32BIT
                else -> {
                    Log.e(LOG_TAG, "Unsupported bit depth: $bitsPerSample")
                    _errorMessage.postValue("Unsupported bit depth: $bitsPerSample")
                    return false
                }
            }

            minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufSize == AudioRecord.ERROR_BAD_VALUE || minBufSize == AudioRecord.ERROR) {
                Log.e(LOG_TAG, "Invalid audio parameters, getMinBufferSize returned error")
                _errorMessage.postValue("Invalid audio parameters")
                return false
            }
            Log.d(LOG_TAG, "Min buffer size: $minBufSize")

            Log.d(LOG_TAG, "Creating AudioRecord instance with buffer size: ${minBufSize * MIN_BUF_MULTIPLIER}")
            audioRecord = AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * MIN_BUF_MULTIPLIER)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(LOG_TAG, "Failed to initialize AudioRecord")
                _errorMessage.postValue("Failed to initialize AudioRecord")
                return false
            }
            Log.d(LOG_TAG, "AudioRecord initialized successfully")

            audioRecord?.startRecording()
            isRecording.set(true)
            _recorderState.postValue(RecorderState.RECORDING)
            Log.i(LOG_TAG, "Recording started successfully")
            
            startRecordingThread()
            
            return true

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error starting recording", e)
            _errorMessage.postValue("Error starting recording: ${e.message}")
            return false
        }
    }

    fun stopRecording(): Boolean {
        if (!isRecording.get()) {
            Log.d(LOG_TAG, "Not recording, ignoring stop request")
            return false
        }

        try {
            Log.d(LOG_TAG, "Stopping recording, total bytes read: $totalBytesRead")
            isRecording.set(false)
            
            recordingThread?.join(1000)
            recordingThread = null
            Log.d(LOG_TAG, "Recording thread stopped")
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(LOG_TAG, "AudioRecord released")
            
            waveFile?.close()
            waveFile = null
            Log.d(LOG_TAG, "WAV file closed")
            
            _recorderState.postValue(RecorderState.IDLE)
            Log.i(LOG_TAG, "Recording stopped successfully")
            
            return true

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error stopping recording", e)
            _errorMessage.postValue("Error stopping recording: ${e.message}")
            return false
        }
    }

    private fun startRecordingThread() {
        recordingThread = Thread {
            val buffer = ByteArray(minBufSize)
            totalBytesRead = 0
            
            while (isRecording.get() && audioRecord != null) {
                try {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        waveFile?.writeAudioData(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        // 每读取一定量数据记录一次，避免日志过多
                        if (totalBytesRead % (minBufSize * 100) == 0) {
                            Log.d(LOG_TAG, "Recording in progress, total read: ${totalBytesRead}bytes, ${totalBytesRead/1024/1024}MB")
                        }
                    } else if (bytesRead < 0) {
                        Log.w(LOG_TAG, "AudioRecord read returned error code: $bytesRead")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Recording error", e)
                    _errorMessage.postValue("Recording error: ${e.message}")
                    break
                }
            }
        }
        recordingThread?.start()
        Log.d(LOG_TAG, "Recording thread started")
    }

    fun release() {
        Log.d(LOG_TAG, "Releasing AudioRecorder resources")
        stopRecording()
    }
    
    /**
     * 生成输出文件路径
     */
    @SuppressLint("SimpleDateFormat")
    private fun generateOutputFilePath(): String {
        val filesDir = context.filesDir
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val dateTime = dateFormat.format(Date())
        val fileName = "recording_${sampleRate}Hz_${channels}ch_${bitsPerSample}bit_${dateTime}.wav"
        return File(filesDir, fileName).absolutePath
    }
}
