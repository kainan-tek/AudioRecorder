package com.example.audiorecorder.model

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * 简洁的WAV文件写入类
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
    
    // 音频参数
    var sampleRate: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var bitsPerSample: Int = 0
        private set

    /**
     * 创建WAV文件并写入文件头
     */
    fun create(sampleRate: Int, channelCount: Int, bitsPerSample: Int): Boolean {
        Log.d(TAG, "创建WAV文件: $filePath")
        
        if (!validateParameters(sampleRate, channelCount, bitsPerSample)) {
            return false
        }
        
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        this.bitsPerSample = bitsPerSample
        
        return try {
            close() // 确保之前的资源已释放
            
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            fileOutputStream = FileOutputStream(file)
            writeInitialWavHeader() // 写入初始头部，数据长度为0
            
            isFileOpen = true
            totalAudioLength = 0
            
            Log.i(TAG, "WAV文件创建成功: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
            true
        } catch (e: IOException) {
            Log.e(TAG, "创建文件失败: $filePath", e)
            close()
            false
        }
    }

    /**
     * 写入音频数据
     */
    fun writeAudioData(audioData: ByteArray, offset: Int, length: Int): Boolean {
        if (!isFileOpen || fileOutputStream == null) {
            return false
        }
        
        if (offset < 0 || length < 0 || offset + length > audioData.size) {
            Log.w(TAG, "无效的写入参数")
            return false
        }
        
        return try {
            fileOutputStream!!.write(audioData, offset, length)
            totalAudioLength += length
            true
        } catch (e: IOException) {
            Log.e(TAG, "写入数据失败", e)
            close()
            false
        }
    }

    /**
     * 关闭文件并更新WAV头信息
     */
    fun close(): Boolean {
        if (isFileOpen || fileOutputStream != null) {
            Log.d(TAG, "关闭WAV文件，总长度: ${totalAudioLength}字节")
            
            try {
                fileOutputStream?.close()
                if (isFileOpen) {
                    updateWavHeader()
                }
                return true
            } catch (e: IOException) {
                Log.e(TAG, "关闭文件失败", e)
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
            Log.e(TAG, "无效的音频参数: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
            return false
        }
        
        if (bitsPerSample !in arrayOf(8, 16, 24, 32)) {
            Log.e(TAG, "不支持的位深度: ${bitsPerSample}bit")
            return false
        }
        
        if (sampleRate !in 8000..192000) {
            Log.w(TAG, "采样率超出常见范围: ${sampleRate}Hz")
        }
        
        if (channelCount > 16) {
            Log.e(TAG, "声道数超出支持范围: ${channelCount}声道")
            return false
        }
        
        return true
    }

    private fun writeInitialWavHeader() {
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8

        val header = ByteArray(WAV_HEADER_SIZE).apply {
            // RIFF header - 初始时数据长度设为0，稍后更新
            "RIFF".toByteArray().copyInto(this, 0)
            writeLittleEndianInt(WAV_HEADER_SIZE - 8, 4) // 临时值，稍后更新
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

            // data subchunk - 初始时数据长度设为0，稍后更新
            "data".toByteArray().copyInto(this, 36)
            writeLittleEndianInt(0, 40) // 数据长度，稍后更新
        }
        
        fileOutputStream?.write(header)
    }

    private fun updateWavHeader() {
        try {
            RandomAccessFile(File(filePath), "rw").use { raf ->
                val totalDataLength = totalAudioLength + WAV_HEADER_SIZE - 8
                // 更新文件总大小
                raf.seek(4)
                raf.write(createLittleEndianInt(totalDataLength))
                // 更新音频数据大小
                raf.seek(40)
                raf.write(createLittleEndianInt(totalAudioLength))
            }
            Log.d(TAG, "WAV头部更新完成")
        } catch (e: IOException) {
            Log.w(TAG, "更新WAV头部失败", e)
        }
    }

    // 辅助方法
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