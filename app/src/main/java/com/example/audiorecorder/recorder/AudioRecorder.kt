package com.example.audiorecorder.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.model.WaveFile
import com.example.audiorecorder.utils.AudioConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Recording state enumeration
 */
enum class RecorderState {
    IDLE,       // Idle state
    RECORDING,  // Recording
    ERROR       // Error state
}

/**
 * Audio recorder core class
 * Supports configurable recording parameters
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var waveFile: WaveFile? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO)
    private var currentConfig: AudioConfig = AudioConfig()

    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onRecordingError(error: String)
    }

    private var listener: RecordingListener? = null

    fun setRecordingListener(listener: RecordingListener) {
        this.listener = listener
    }
    
    fun setAudioConfig(config: AudioConfig) {
        if (isRecording.get()) {
            Log.w(TAG, "Cannot change configuration while recording")
            return
        }
        currentConfig = config
        Log.i(TAG, "Configuration updated: ${config.description}")
    }

    fun startRecording(): Boolean {
        // Explicitly check recording permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            handleError("Recording permission not granted")
            return false
        }
        
        if (isRecording.get()) {
            stopRecording()
        }

        return try {
            if (createOutputFile() && initializeAudioRecord()) {
                isRecording.set(true)
                startRecordingLoop()
                listener?.onRecordingStarted()
                Log.i(TAG, "Recording started successfully")
                true
            } else false
        } catch (_: SecurityException) {
            handleError("Recording permission denied")
            false
        } catch (e: Exception) {
            handleError("Recording initialization failed: ${e.message}")
            false
        }
    }

    fun stopRecording() {
        if (!isRecording.get()) return

        isRecording.set(false)
        recordingJob?.cancel()
        releaseResources()
        listener?.onRecordingStopped()
        Log.i(TAG, "Recording stopped")
    }

    fun release() {
        stopRecording()
        listener = null  // Clear listener reference to prevent memory leaks
        recordingScope.cancel()
    }

    private fun createOutputFile(): Boolean {
        val outputPath = currentConfig.audioFilePath.takeIf { it.isNotEmpty() } 
            ?: generateOutputFilePath()
        
        waveFile = WaveFile(outputPath)
        val channelCount = currentConfig.channelCount // Directly use channel count from configuration
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        
        return if (waveFile!!.create(currentConfig.sampleRate, channelCount, bitsPerSample)) {
            Log.d(TAG, "Output file created: $outputPath (${channelCount} channels)")
            true
        } else {
            handleError("Cannot create output file: $outputPath")
            false
        }
    }

    private fun initializeAudioRecord(): Boolean {
        return try {
            if (!validateAudioParameters()) return false

            val minBufferSize = AudioRecord.getMinBufferSize(
                currentConfig.sampleRate, 
                currentConfig.channelMask, 
                currentConfig.audioFormat
            )
            
            if (minBufferSize <= 0) {
                handleError("Unsupported audio parameter combination")
                return false
            }
            
            val bufferSize = maxOf(
                minBufferSize * currentConfig.bufferMultiplier, 
                currentConfig.minBufferSize
            )

            audioRecord = AudioRecord.Builder()
                .setAudioSource(currentConfig.audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(currentConfig.sampleRate)
                        .setChannelMask(currentConfig.channelMask)
                        .setEncoding(currentConfig.audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                handleError("AudioRecord initialization failed")
                return false
            }

            Log.i(TAG, "AudioRecord initialized successfully - ${currentConfig.description}")
            true
        } catch (_: SecurityException) {
            handleError("Recording permission denied")
            false
        } catch (e: Exception) {
            handleError("AudioRecord creation failed: ${e.message}")
            false
        }
    }

    private fun validateAudioParameters(): Boolean {
        val sampleRate = currentConfig.sampleRate
        val channelCount = currentConfig.channelCount // Directly use channel count from configuration
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        
        return when {
            sampleRate !in 8000..192000 -> {
                handleError("Unsupported sample rate: ${sampleRate}Hz")
                false
            }
            channelCount !in 1..16 -> {
                handleError("Unsupported channel count: $channelCount")
                false
            }
            bitsPerSample !in listOf(8, 16, 24, 32) -> {
                handleError("Unsupported bit depth: ${bitsPerSample}bit")
                false
            }
            else -> true
        }
    }

    private fun startRecordingLoop() {
        recordingJob = recordingScope.launch {
            // Adjust buffer size based on channel count
            val baseBufferSize = when {
                currentConfig.channelCount >= 12 -> AudioConstants.BUFFER_SIZE_12CH
                currentConfig.channelCount >= 8 -> AudioConstants.BUFFER_SIZE_8CH
                currentConfig.channelCount >= 6 -> AudioConstants.BUFFER_SIZE_6CH
                else -> AudioConstants.BUFFER_SIZE_DEFAULT
            }

            val buffer = ByteArray(baseBufferSize)
            var totalBytes = 0L
            
            try {
                audioRecord?.startRecording()
                Log.i(TAG, "Started recording - ${currentConfig.description}, buffer: $baseBufferSize bytes")
                
                while (isActive && isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    
                    if (bytesRead <= 0) {
                        Log.w(TAG, "AudioRecord read failed or reached end: $bytesRead")
                        break
                    }
                    
                    waveFile?.writeAudioData(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    // Periodically output recording progress
                    if (totalBytes % AudioConstants.PROGRESS_LOG_INTERVAL == 0L && totalBytes > 0) {
                        val mbRecorded = totalBytes / (1024.0 * 1024.0)
                        Log.d(TAG, "Recording progress: ${String.format("%.1f", mbRecorded)}MB")
                    }
                }
            } catch (_: SecurityException) {
                if (isRecording.get()) {
                    handleError("Recording permission denied")
                }
            } catch (e: Exception) {
                if (isRecording.get()) {
                    handleError("Recording error: ${e.message}")
                }
            }
        }
    }

    private fun releaseResources() {
        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) stop()
                release()
            }
            audioRecord = null
            waveFile?.close()
            waveFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }

    private fun handleError(message: String) {
        Log.e(TAG, message)
        listener?.onRecordingError(message)
        releaseResources()
    }

    private fun getBitsPerSample(audioFormat: Int) = when (audioFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> 8
        AudioFormat.ENCODING_PCM_16BIT -> 16
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
        AudioFormat.ENCODING_PCM_32BIT -> 32
        else -> 16
    }
    
    @SuppressLint("SimpleDateFormat")
    private fun generateOutputFilePath(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        val dateTime = dateFormat.format(Date())
        val channelCount = currentConfig.channelCount // Directly use channel count from configuration
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        val fileName = "recording_${currentConfig.sampleRate}Hz_${channelCount}ch_${bitsPerSample}bit_${dateTime}.wav"
        return File(context.filesDir, fileName).absolutePath
    }
}
