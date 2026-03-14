package com.example.audiorecorder.recorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import com.example.audiorecorder.common.AudioConstants
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.util.WavFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RecorderState {
    IDLE, RECORDING, ERROR
}

/**
 * Audio recorder using AudioRecord API
 */
class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var wavFile: WavFile? = null

    @Volatile
    private var state = RecorderState.IDLE
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO)
    private var currentConfig: AudioConfig = AudioConfig()

    // Recording listener
    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingStopped()
        fun onRecordingError(error: String)
    }

    private var listener: RecordingListener? = null

    fun setRecordingListener(listener: RecordingListener?) {
        this.listener = listener
    }

    fun setAudioConfig(config: AudioConfig) {
        if (state == RecorderState.RECORDING) {
            Log.w(TAG, "Cannot change configuration while recording")
            return
        }
        currentConfig = config
        Log.i(TAG, "Configuration updated: ${config.description}")
    }

    fun startRecording(): Boolean {
        Log.d(TAG, "Starting recording")

        if (state == RecorderState.RECORDING) {
            Log.w(TAG, "Already recording")
            listener?.onRecordingError("Already recording")
            return false
        }
        if (state == RecorderState.ERROR) {
            state = RecorderState.IDLE
        }

        return try {
            if (!createOutputFile()) {
                return false
            }
            if (!initializeAudioRecord()) {
                return false
            }

            state = RecorderState.RECORDING
            startRecordingLoop()
            listener?.onRecordingStarted()

            Log.i(TAG, "Recording started successfully")
            true
        } catch (e: SecurityException) {
            handleError("${AudioConstants.ErrorTypes.PERMISSION} Recording permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            handleError("${AudioConstants.ErrorTypes.STREAM} Recording initialization failed: ${e.message}")
            false
        }
    }

    fun stopRecording() {
        Log.d(TAG, "Stopping recording")

        if (state != RecorderState.RECORDING) {
            return
        }

        state = RecorderState.IDLE
        recordingJob?.cancel()
        releaseResources()
        listener?.onRecordingStopped()

        Log.i(TAG, "Recording stopped")
    }

    fun release() {
        stopRecording()
        listener = null
        try {
            recordingScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error canceling recording scope", e)
        }
        Log.d(TAG, "AudioRecorder resources released")
    }

    fun isRecording(): Boolean {
        return state == RecorderState.RECORDING
    }

    private fun createOutputFile(): Boolean {
        val outputPath =
            currentConfig.audioFilePath.takeIf { it.isNotEmpty() } ?: generateOutputFilePath()

        return try {
            wavFile = WavFile(outputPath)
            val channelCount = currentConfig.channelCount
            val bitsPerSample = currentConfig.audioFormat

            if (wavFile!!.create(currentConfig.sampleRate, channelCount, bitsPerSample)) {
                Log.d(TAG, "Output file created: $outputPath (${channelCount} channels)")
                true
            } else {
                val file = File(outputPath)
                val parentDir = file.parentFile
                val errorMsg = if (parentDir != null && !parentDir.canWrite()) {
                    "${AudioConstants.ErrorTypes.FILE} No write permission for directory: ${parentDir.absolutePath}"
                } else {
                    "${AudioConstants.ErrorTypes.FILE} Cannot create output file: $outputPath"
                }
                handleError(errorMsg)
                false
            }
        } catch (e: SecurityException) {
            handleError("${AudioConstants.ErrorTypes.PERMISSION} Permission denied when creating file: $outputPath - ${e.message}")
            false
        } catch (e: Exception) {
            handleError("${AudioConstants.ErrorTypes.FILE} Failed to create output file: $outputPath - ${e.message}")
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
                handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported audio parameter combination")
                return false
            }

            val bufferSize = minBufferSize * currentConfig.bufferMultiplier

            audioRecord = AudioRecord.Builder()
                .setAudioSource(AudioConstants.getAudioSource(currentConfig.audioSource))
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(currentConfig.sampleRate)
                        .setChannelMask(AudioConstants.getChannelMask(currentConfig.channelCount))
                        .setEncoding(AudioConstants.getFormatFromBitDepth(currentConfig.audioFormat))
                        .build()
                ).setBufferSizeInBytes(bufferSize).build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                handleError("${AudioConstants.ErrorTypes.STREAM} AudioRecord initialization failed")
                return false
            }

            Log.i(TAG, "AudioRecord initialized successfully - ${currentConfig.description}")
            true
        } catch (_: SecurityException) {
            handleError("${AudioConstants.ErrorTypes.PERMISSION} Recording permission denied")
            false
        } catch (e: Exception) {
            handleError("${AudioConstants.ErrorTypes.STREAM} AudioRecord creation failed: ${e.message}")
            false
        }
    }

    private fun validateAudioParameters(): Boolean {
        val sampleRate = currentConfig.sampleRate
        val channelCount = currentConfig.channelCount
        val bitsPerSample = currentConfig.audioFormat

        return when {
            !AudioConstants.isValidSampleRate(sampleRate) -> {
                handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported sample rate: ${sampleRate}Hz")
                false
            }

            !AudioConstants.isValidChannelCount(channelCount) -> {
                handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported channel count: $channelCount")
                false
            }

            !AudioConstants.isValidBitDepth(bitsPerSample) -> {
                handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported bit depth: ${bitsPerSample}bit")
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
            val audioRecordBufferSize =
                audioRecord.bufferSizeInFrames * currentConfig.channelCount * (currentConfig.audioFormat / 8)
            val readBufferSize = audioRecordBufferSize / 3  // Use 1/3 of AudioRecord buffer

            val buffer = ByteArray(readBufferSize)
            var totalBytes = 0L

            try {
                audioRecord.startRecording()
                Log.i(TAG, "Started recording - ${currentConfig.description}")

                while (isActive && state == RecorderState.RECORDING) {
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (bytesRead <= 0) {
                        Log.w(TAG, "AudioRecord read failed or reached end: $bytesRead")
                        break
                    }

                    wavFile?.writeAudioData(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    // Log progress every 5MB
                    if (totalBytes % (5 * 1024 * 1024L) == 0L && totalBytes > 0) {
                        val mbRecorded = totalBytes / (1024.0 * 1024.0)
                        Log.v(TAG, "Progress: %.1fMB".format(mbRecorded))
                    }
                }

                if (state == RecorderState.RECORDING) {
                    val mbRecorded = totalBytes / (1024.0 * 1024.0)
                    Log.i(TAG, "Recording completed: %.1fMB".format(mbRecorded))
                    stopRecording()
                }
            } catch (e: SecurityException) {
                if (state == RecorderState.RECORDING) {
                    handleError("${AudioConstants.ErrorTypes.PERMISSION} Recording permission denied: ${e.message}")
                }
            } catch (e: Exception) {
                if (state == RecorderState.RECORDING) {
                    handleError("${AudioConstants.ErrorTypes.STREAM} Recording error: ${e.message}")
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
                if (this.state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null

            wavFile?.close()
            wavFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }

    private fun handleError(message: String) {
        state = RecorderState.ERROR
        Log.e(TAG, "Error: $message")
        listener?.onRecordingError(message)
        releaseResources()
    }

    private fun generateOutputFilePath(): String {
        val directory =
            context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        val dateTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val channelCount = currentConfig.channelCount
        val bitsPerSample = currentConfig.audioFormat
        val sampleRateK = currentConfig.sampleRate / 1000
        val fileName = "rec_${dateTime}_${sampleRateK}k_${channelCount}ch_${bitsPerSample}bit.wav"
        return File(directory, fileName).absolutePath
    }
}
