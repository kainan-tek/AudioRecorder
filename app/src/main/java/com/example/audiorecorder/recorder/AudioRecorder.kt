package com.example.audiorecorder.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.audiorecorder.model.WaveFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * status of recorder
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
    // 如果audioRecordFile为空，就使用默认的文件路径，自动生成文件
    private var audioRecordFile: String = "/data/record_48k_1ch_16bit.wav"

    private var audioRecord: AudioRecord? = null
    private var waveFile: WaveFile? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    
    private val _recorderState = MutableLiveData<RecorderState>()
    val recorderState: LiveData<RecorderState> = _recorderState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        _recorderState.value = RecorderState.IDLE
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(): Boolean {
        Log.d(LOG_TAG, "startRecording() called")
        if (isRecording.get()) {
            Log.w(LOG_TAG, "Already recording")
            return false
        }

        try {
            // 确定输出文件路径
            val outputFilePath = audioRecordFile.ifEmpty { generateOutputFilePath() }
            Log.d(LOG_TAG, "Output file path: $outputFilePath")
            
            // 初始化WAV文件
            waveFile = WaveFile(outputFilePath)
            if (waveFile?.create(sampleRate, channels, bitsPerSample) != true) {
                // 检查文件是否存在
                val file = File(outputFilePath)
                val msg = if (file.exists()) {
                    "File exists but access is denied"
                } else {
                    "File does not exist"
                }
                Log.e(LOG_TAG, msg)
                _errorMessage.postValue(msg)
                return false
            }

            // 计算缓冲区大小
            val channelConfig = when (channels) {
                1 -> AudioFormat.CHANNEL_IN_MONO
                2 -> AudioFormat.CHANNEL_IN_STEREO
                10 -> CHANNEL_IN_10
                12 -> CHANNEL_IN_12
                14 -> CHANNEL_IN_14
                16 -> CHANNEL_IN_16
                else -> {
                    _errorMessage.postValue("Unsupported channel count: $channels")
                    return false
                }
            }
            Log.d(LOG_TAG, "Audio parameters - Sample rate: $sampleRate, Channels: $channels, Bits per sample: $bitsPerSample")
                
            // 根据位深度选择适当的编码格式
            val audioFormat = when (bitsPerSample) {
                8 -> AudioFormat.ENCODING_PCM_8BIT
                16 -> AudioFormat.ENCODING_PCM_16BIT
                24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
                32 -> AudioFormat.ENCODING_PCM_32BIT
                else -> {
                    _errorMessage.postValue("Unsupported bit depth: $bitsPerSample")
                    return false
                }
            }

            minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            Log.d(LOG_TAG, "Calculated min buffer size: $minBufSize")
            if (minBufSize == AudioRecord.ERROR_BAD_VALUE || minBufSize == AudioRecord.ERROR) {
                _errorMessage.postValue("Invalid audio parameters")
                return false
            }

            // 创建AudioRecord实例
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
                _errorMessage.postValue("Failed to initialize AudioRecord")
                return false
            }

            // 开始录音
            audioRecord?.startRecording()
            isRecording.set(true)
            Log.i(LOG_TAG, "Recording started successfully")
            _recorderState.postValue(RecorderState.RECORDING)
            
            startRecordingThread()
            
            return true

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error starting recording", e)
            _errorMessage.postValue("Error starting recording: ${e.message}")
            return false
        }
    }

    fun stopRecording(): Boolean {
        Log.d(LOG_TAG, "stopRecording() called")
        if (!isRecording.get()) {
            Log.w(LOG_TAG, "Not currently recording")
            return false
        }

        try {
            isRecording.set(false)
            Log.d(LOG_TAG, "Stopping recording thread")
            
            recordingThread?.join(1000)
            recordingThread = null
            Log.d(LOG_TAG, "Recording thread stopped")
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            waveFile?.close()
            waveFile = null
            
            _recorderState.postValue(RecorderState.IDLE)
            Log.i(LOG_TAG, "Recording stopped successfully, totalBytesRead: $totalBytesRead")
            
            return true

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error stopping recording", e)
            _errorMessage.postValue("Error stopping recording: ${e.message}")
            return false
        }
    }

    private fun startRecordingThread() {
        Log.d(LOG_TAG, "Starting recording thread")
        recordingThread = Thread {
            val buffer = ByteArray(minBufSize)
            totalBytesRead = 0
            
            while (isRecording.get() && audioRecord != null) {
                try {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        waveFile?.writeAudioData(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        // Log.v(LOG_TAG, "Wrote $bytesRead bytes to file")
                    } else if (bytesRead < 0) {
                        Log.e(LOG_TAG, "Error: AudioRecord read returned $bytesRead")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error in recording thread", e)
                    _errorMessage.postValue("Recording error: ${e.message}")
                    break
                }
            }
        }
        recordingThread?.start()
    }


    fun release() {
        stopRecording()
    }
    
    /**
     * 生成输出文件路径
     * 格式: /data/user/10/com.example.audiorecorder/files/recording_<sampleRate>_<channels>_<bitsPerSample>_<dateTime>.wav
     */
    @SuppressLint("SimpleDateFormat")
    private fun generateOutputFilePath(): String {
        // 获取应用的内部存储目录
        val filesDir = context.filesDir
        // 格式化当前时间为可读格式：年月日时分秒
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val dateTime = dateFormat.format(Date())
        val fileName = "recording_${sampleRate}Hz_${channels}ch_${bitsPerSample}bit_${dateTime}.wav"
        // 组合目录和文件名
        val filePath = File(filesDir, fileName).absolutePath
        Log.d(LOG_TAG, "Generated output file path: $filePath")
        return filePath
    }
}
