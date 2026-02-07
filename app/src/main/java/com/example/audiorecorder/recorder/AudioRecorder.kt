package com.example.audiorecorder.recorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import com.example.audiorecorder.common.AudioConstants
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.model.WaveFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
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

    // Recording components
    private var audioRecord: AudioRecord? = null
    private var waveFile: WaveFile? = null
    
    // Recording state
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO)
    
    // Audio configuration
    private var currentConfig: AudioConfig = AudioConfig()

    // Recording listener
    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onRecordingError(error: String)
    }

    private var listener: RecordingListener? = null

    fun setRecordingListener(listener: RecordingListener) {
        this.listener = listener
    }
    
    /**
     * Set audio configuration
     */
    fun setAudioConfig(config: AudioConfig) {
        if (isRecording.get()) {
            Log.w(TAG, "Cannot change configuration while recording")
            return
        }
        currentConfig = config
        Log.i(TAG, "Configuration updated: ${config.description}")
        Log.d(TAG, getDetailedInfo())
    }

    /**
     * Start audio recording
     */
    fun startRecording(): Boolean {
        Log.d(TAG, "Starting recording")
        
        if (isRecording.get()) {
            Log.i(TAG, "Already recording, stopping current recording first")
            stopRecording()
        }

        return try {
            // Create output file
            if (!createOutputFile()) {
                return false
            }
            
            // Initialize recorder
            if (!initializeAudioRecord()) {
                return false
            }
            
            // Start recording
            isRecording.set(true)
            startRecordingLoop()
            listener?.onRecordingStarted()
            
            Log.i(TAG, "Recording started successfully")
            true
        } catch (e: SecurityException) {
            handleError("Recording permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            handleError("Recording initialization failed: ${e.message}")
            false
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        Log.d(TAG, "Stopping recording")
        
        if (!isRecording.get()) {
            return
        }

        isRecording.set(false)
        recordingJob?.cancel()
        releaseResources()
        listener?.onRecordingStopped()
        
        Log.i(TAG, "Recording stopped")
    }

    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing recorder resources")
        stopRecording()
        listener = null  // Clear listener reference to prevent memory leaks
        try {
            recordingScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error canceling recording scope", e)
        }
    }

    private fun createOutputFile(): Boolean {
        val outputPath = currentConfig.audioFilePath.takeIf { it.isNotEmpty() } 
            ?: generateOutputFilePath()
        
        return try {
            waveFile = WaveFile(outputPath)
            val channelCount = currentConfig.channelCount // Directly use channel count from configuration
            val bitsPerSample = currentConfig.audioFormat
            
            if (waveFile!!.create(currentConfig.sampleRate, channelCount, bitsPerSample)) {
                Log.d(TAG, "Output file created: $outputPath (${channelCount} channels)")
                true
            } else {
                val file = File(outputPath)
                val parentDir = file.parentFile
                val errorMsg = if (parentDir != null && !parentDir.canWrite()) {
                    "No write permission for directory: ${parentDir.absolutePath}"
                } else {
                    "Cannot create output file: $outputPath"
                }
                handleError(errorMsg)
                false
            }
        } catch (e: SecurityException) {
            handleError("Permission denied when creating file: $outputPath - ${e.message}")
            false
        } catch (e: Exception) {
            handleError("Failed to create output file: $outputPath - ${e.message}")
            false
        }
    }

    private fun initializeAudioRecord(): Boolean {
        return try {
            if (!validateAudioParameters()) return false

            val minBufferSize = AudioRecord.getMinBufferSize(
                currentConfig.sampleRate,
                AudioConstants.getChannelMask(currentConfig.channelCount),
                AudioConstants.getFormatFromBitDepth(currentConfig.audioFormat)
            )
            if (minBufferSize <= 0) {
                handleError("Unsupported audio parameter combination")
                return false
            }
            
            val bufferSize = minBufferSize * currentConfig.bufferMultiplier
            Log.d(TAG, "Buffer calculation: minBufferSize=$minBufferSize, multiplier=${currentConfig.bufferMultiplier}, final=$bufferSize")

            audioRecord = AudioRecord.Builder()
                .setAudioSource(AudioConstants.getAudioSource(currentConfig.audioSource))
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(currentConfig.sampleRate)
                        .setChannelMask(AudioConstants.getChannelMask(currentConfig.channelCount))
                        .setEncoding(AudioConstants.getFormatFromBitDepth(currentConfig.audioFormat))
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
        val bitsPerSample = currentConfig.audioFormat
        
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
            val audioRecord = audioRecord ?: return@launch
            
            // Use a read buffer that's a fraction of the AudioRecord's internal buffer
            // This ensures smooth recording without overruns
            val audioRecordBufferSize = audioRecord.bufferSizeInFrames * currentConfig.channelCount * (currentConfig.audioFormat / 8)
            val readBufferSize = audioRecordBufferSize / 3  // Use 1/3 of AudioRecord buffer
            
            val buffer = ByteArray(readBufferSize)
            var totalBytes = 0L
            
            try {
                audioRecord.startRecording()
                Log.i(TAG, "Started recording - ${currentConfig.description}")
                Log.d(TAG, "AudioRecord buffer: $audioRecordBufferSize bytes, Read buffer: $readBufferSize bytes")
                
                while (isActive && isRecording.get()) {
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (bytesRead <= 0) {
                        Log.w(TAG, "AudioRecord read failed or reached end: $bytesRead")
                        break
                    }
                    
                    waveFile?.writeAudioData(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    // Periodically output recording progress (every 1MB)
                    if (totalBytes % (1024 * 1024L) == 0L && totalBytes > 0) {
                        val mbRecorded = totalBytes / (1024.0 * 1024.0)
                        Log.d(TAG, "Recording progress: ${String.format(java.util.Locale.US, "%.1f", mbRecorded)}MB")
                    }
                }
                
                if (isRecording.get()) {
                    val mbRecorded = totalBytes / (1024.0 * 1024.0)
                    Log.i(TAG, "Recording completed: ${String.format(java.util.Locale.US, "%.1f", mbRecorded)}MB")
                    stopRecording()
                }
            } catch (e: SecurityException) {
                if (isRecording.get()) {
                    handleError("Recording permission denied: ${e.message}")
                }
            } catch (e: Exception) {
                if (isRecording.get()) {
                    handleError("Recording error: ${e.message}")
                }
            }
        }
    }

    /**
     * Release audio resources consistently
     */
    private fun releaseResources() {
        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null

            waveFile?.close()
            waveFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }

    /**
     * Handle errors consistently
     */
    private fun handleError(message: String) {
        isRecording.set(false)  // Stop recording on error
        Log.e(TAG, "Error: $message")
        listener?.onRecordingError(message)
        releaseResources()
    }
    
    private fun generateOutputFilePath(): String {
        val dateTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val channelCount = currentConfig.channelCount
        val bitsPerSample = currentConfig.audioFormat
        val fileName = "recording_${currentConfig.sampleRate}Hz_${channelCount}ch_${bitsPerSample}bit_${dateTime}.wav"
        return File(context.filesDir, fileName).absolutePath
    }

    /**
     * Get detailed configuration information
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("Configuration: ${currentConfig.description}")
            appendLine("Audio Source: ${currentConfig.audioSource}")
            appendLine("Sample Rate: ${currentConfig.sampleRate}Hz")
            appendLine("Channel Count: ${currentConfig.channelCount}")
            appendLine("Audio Format: ${currentConfig.audioFormat}bit")
            appendLine("Buffer Multiplier: ${currentConfig.bufferMultiplier}x")
            appendLine("Output File: ${currentConfig.audioFilePath}")
        }.trim()
    }
}
