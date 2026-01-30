package com.example.audiorecorder.model

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * WAV file writer for audio recording
 * Handles WAV file creation and audio data writing with proper header management
 */
class WaveFile(private val filePath: String) {
    
    companion object {
        private const val TAG = "WaveFile"
        private const val WAV_HEADER_SIZE = 44
        private const val FMT_CHUNK_SIZE = 16
        private const val AUDIO_FORMAT_PCM = 1  // PCM format
    }

    private var fileOutputStream: FileOutputStream? = null
    private var isFileOpen = false
    private var totalAudioLength = 0
    
    // Audio properties
    var sampleRate: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var bitsPerSample: Int = 0
        private set
    
    // Computed properties for consistency with AudioPlayer
    val byteRate: Int
        get() = sampleRate * channelCount * bitsPerSample / 8
    
    val blockAlign: Int
        get() = channelCount * bitsPerSample / 8
    
    /**
     * Calculate audio duration based on current data length
     */
    val duration: Float
        get() = if (sampleRate > 0 && channelCount > 0 && bitsPerSample > 0) {
            totalAudioLength.toFloat() / (sampleRate * channelCount * (bitsPerSample / 8))
        } else 0f

    /**
     * Create WAV file and write file header
     */
    fun create(sampleRate: Int, channelCount: Int, bitsPerSample: Int): Boolean {
        Log.d(TAG, "Creating WAV file: $filePath")
        
        if (!validateParameters(sampleRate, channelCount, bitsPerSample)) {
            return false
        }
        
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        this.bitsPerSample = bitsPerSample
        
        return try {
            close() // Ensure previous resources are released
            
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            fileOutputStream = FileOutputStream(file)
            writeInitialWavHeader() // Write initial header with data length 0
            
            isFileOpen = true
            totalAudioLength = 0
            
            Log.i(TAG, "WAV file created successfully: ${sampleRate}Hz, ${getChannelDescription()}ch, ${bitsPerSample}bit")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create file: $filePath", e)
            close()
            false
        }
    }

    /**
     * Write audio data
     */
    fun writeAudioData(audioData: ByteArray, offset: Int, length: Int): Boolean {
        if (!isFileOpen || fileOutputStream == null) {
            return false
        }
        
        if (offset < 0 || length < 0 || offset + length > audioData.size) {
            Log.w(TAG, "Invalid write parameters")
            return false
        }
        
        return try {
            fileOutputStream!!.write(audioData, offset, length)
            totalAudioLength += length
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write data", e)
            close()
            false
        }
    }

    /**
     * Close file and update WAV header information
     */
    fun close(): Boolean {
        if (isFileOpen || fileOutputStream != null) {
            Log.d(TAG, "Closing WAV file: $totalAudioLength bytes, ${String.format(java.util.Locale.US, "%.2f", duration)}s")
            
            try {
                fileOutputStream?.close()
                if (isFileOpen) {
                    updateWavHeader()
                }
                return true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to close file", e)
                return false
            } finally {
                fileOutputStream = null
                isFileOpen = false
            }
        }
        return true
    }

    /**
     * Validate audio parameters with comprehensive checks
     */
    private fun validateParameters(sampleRate: Int, channelCount: Int, bitsPerSample: Int): Boolean {
        // Basic parameter check
        if (sampleRate <= 0 || channelCount <= 0 || bitsPerSample <= 0) {
            Log.e(TAG, "Invalid audio parameters: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
            return false
        }
        
        // Check bit depth support
        if (bitsPerSample !in listOf(8, 16, 24, 32)) {
            Log.e(TAG, "Unsupported bit depth: ${bitsPerSample}bit (supported: 8/16/24/32bit)")
            return false
        }
        
        // Check sample rate range
        if (sampleRate !in 8000..192000) {
            Log.w(TAG, "Sample rate outside common range: ${sampleRate}Hz (supported: 8000-192000Hz)")
        }
        
        // Check channel count
        if (channelCount > 16) {
            Log.e(TAG, "Channel count exceeds supported range: $channelCount channels (supported: 1-16)")
            return false
        } else if (channelCount > 12) {
            Log.w(TAG, "High channel count: $channelCount channels, may not be supported by all devices")
        } else if (channelCount == 12) {
            Log.i(TAG, "Detected 7.1.4 audio format (12 channels)")
        }
        
        return true
    }
    
    /**
     * Get channel description string
     */
    private fun getChannelDescription(): String = when (channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        4 -> "Quad"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        10 -> "5.1.4 Surround"
        12 -> "7.1.4 Surround"
        else -> "$channelCount channels"
    }

    private fun writeInitialWavHeader() {
        val header = ByteArray(WAV_HEADER_SIZE).apply {
            // RIFF header - initially set data length to 0, update later
            "RIFF".toByteArray().copyInto(this, 0)
            writeLittleEndianInt(WAV_HEADER_SIZE - 8, 4) // Temporary value, update later
            "WAVE".toByteArray().copyInto(this, 8)

            // fmt subchunk
            "fmt ".toByteArray().copyInto(this, 12)
            writeLittleEndianInt(FMT_CHUNK_SIZE, 16)
            writeLittleEndianShort(AUDIO_FORMAT_PCM, 20)
            writeLittleEndianShort(channelCount, 22)
            writeLittleEndianInt(sampleRate, 24)
            writeLittleEndianInt(byteRate, 28)
            writeLittleEndianShort(blockAlign, 32)
            writeLittleEndianShort(bitsPerSample, 34)

            // data subchunk - initially set data length to 0, update later
            "data".toByteArray().copyInto(this, 36)
            writeLittleEndianInt(0, 40) // Data length, update later
        }
        
        fileOutputStream?.write(header)
    }

    /**
     * Update WAV header with final file sizes
     */
    private fun updateWavHeader() {
        try {
            RandomAccessFile(File(filePath), "rw").use { raf ->
                val totalDataLength = totalAudioLength + WAV_HEADER_SIZE - 8
                // Update total file size
                raf.seek(4)
                raf.write(createLittleEndianInt(totalDataLength))
                // Update audio data size
                raf.seek(40)
                raf.write(createLittleEndianInt(totalAudioLength))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }

    // Helper methods - consistent with AudioPlayer implementation
    private fun ByteArray.writeLittleEndianInt(value: Int, offset: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun ByteArray.writeLittleEndianShort(value: Int, offset: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun createLittleEndianInt(value: Int): ByteArray {
        return ByteArray(4).apply { writeLittleEndianInt(value, 0) }
    }
    
    override fun toString(): String {
        return "WaveFile(path='$filePath', ${sampleRate}Hz, ${getChannelDescription()}ch, ${bitsPerSample}bit, ${String.format(java.util.Locale.US, "%.2f", duration)}s)"
    }
}