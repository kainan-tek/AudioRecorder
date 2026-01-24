package com.example.audiorecorder.model

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Concise WAV file writing class
 */
class WaveFile(private val filePath: String) {
    
    companion object {
        private const val TAG = "WaveFile"
        private const val WAV_HEADER_SIZE = 44
        private const val FMT_CHUNK_SIZE = 16
        private const val AUDIO_FORMAT_PCM = 1
    }

    private var fileOutputStream: FileOutputStream? = null
    private var isFileOpen = false
    private var totalAudioLength = 0
    
    // Audio parameters
    var sampleRate: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var bitsPerSample: Int = 0
        private set

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
            
            Log.i(TAG, "WAV file created successfully: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
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
            Log.d(TAG, "Closing WAV file, total length: $totalAudioLength bytes")
            
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

    private fun validateParameters(sampleRate: Int, channelCount: Int, bitsPerSample: Int): Boolean {
        if (sampleRate <= 0 || channelCount <= 0 || bitsPerSample <= 0) {
            Log.e(TAG, "Invalid audio parameters: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
            return false
        }
        
        if (bitsPerSample !in arrayOf(8, 16, 24, 32)) {
            Log.e(TAG, "Unsupported bit depth: ${bitsPerSample}bit")
            return false
        }
        
        if (sampleRate !in 8000..192000) {
            Log.w(TAG, "Sample rate outside common range: ${sampleRate}Hz")
        }
        
        if (channelCount > 16) {
            Log.e(TAG, "Channel count exceeds supported range: $channelCount channels")
            return false
        }
        
        return true
    }

    private fun writeInitialWavHeader() {
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8

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
            Log.d(TAG, "WAV header update completed")
        } catch (e: IOException) {
            Log.w(TAG, "Failed to update WAV header", e)
        }
    }

    // Helper methods
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
}