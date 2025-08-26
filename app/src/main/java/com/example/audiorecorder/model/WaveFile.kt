package com.example.audiorecorder.model

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * WAV文件处理类，负责创建、写入和关闭WAV格式音频文件
 * @param filePath WAV文件保存路径
 */
class WaveFile(private val filePath: String) {
    // 文件相关属性
    private var file: File? = null
    private var fileOutputStream: FileOutputStream? = null
    
    // 音频参数
    private var totalAudioLength = 0
    private var sampleRate = 48000
    private var channels = 1
    private var bitsPerSample = 16
    
    // WAV文件常量
    companion object {
        private const val WAV_HEADER_SIZE = 44
        private const val FMT_CHUNK_SIZE = 16
        private const val AUDIO_FORMAT_PCM = 1
    }

    /**
     * 创建WAV文件并写入文件头
     * @param sampleRate 采样率
     * @param channels 声道数
     * @param bitsPerSample 位深度
     * @return 是否创建成功
     */
    fun create(sampleRate: Int, channels: Int, bitsPerSample: Int): Boolean {
        // 参数验证
        if (sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0 || 
            bitsPerSample !in arrayOf(8, 16, 24, 32)) {
            return false
        }
        
        this.sampleRate = sampleRate
        this.channels = channels
        this.bitsPerSample = bitsPerSample
        
        return try {
            file = File(filePath)
            file?.parentFile?.mkdirs()
            fileOutputStream = FileOutputStream(file)
            // 验证FileOutputStream是否成功创建
            if (fileOutputStream != null) {
                // 实际长度会在录音完成后通过updateWavHeader方法更新
                writeWavHeader(fileOutputStream!!, 0)
                true
            } else {
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 写入音频数据
     * @param audioData 音频数据
     * @param offset 偏移量
     * @param length 数据长度
     * @return 是否写入成功
     */
    fun writeAudioData(audioData: ByteArray, offset: Int, length: Int): Boolean {
        // 参数验证
        if (offset < 0 || length < 0 || offset + length > audioData.size) {
            return false
        }
        
        return try {
            fileOutputStream?.write(audioData, offset, length)
            totalAudioLength += length
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 关闭文件并更新WAV头信息
     * @return 是否关闭成功
     */
    fun close(): Boolean {
        return try {
            fileOutputStream?.close()
            fileOutputStream = null
            updateWavHeader()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // 生成完整的WAV文件头
    private fun createWavHeader(totalAudioLen: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = totalAudioLen + WAV_HEADER_SIZE - 8

        val header = ByteArray(WAV_HEADER_SIZE)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntToByteArray(totalDataLen, header, 4)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = FMT_CHUNK_SIZE.toByte()
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = AUDIO_FORMAT_PCM.toByte()
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        writeIntToByteArray(sampleRate, header, 24)
        writeIntToByteArray(byteRate, header, 28)
        header[32] = ((channels * bitsPerSample) / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntToByteArray(totalAudioLen, header, 40)

        return header
    }
    
    // 将整数写入字节数组的指定位置
    private fun writeIntToByteArray(value: Int, buffer: ByteArray, offset: Int) {
        buffer[offset] = (value and 0xff).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xff).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xff).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xff).toByte()
    }
    
    // 写入完整的WAV文件头
    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Int) {
        val header = createWavHeader(totalAudioLen)
        out.write(header, 0, WAV_HEADER_SIZE)
    }

    // 更新WAV文件头中的音频数据长度信息
    private fun updateWavHeader() {
        try {
            if (file?.exists() == true) {
                RandomAccessFile(file, "rw").use { randomAccessFile ->
                    // 直接使用类属性totalAudioLength而不是重新计算
                    val totalDataLen = totalAudioLength + WAV_HEADER_SIZE - 8

                    // 更新文件总大小（偏移量4处）
                    randomAccessFile.seek(4)
                    val dataLenBytes = ByteArray(4)
                    writeIntToByteArray(totalDataLen, dataLenBytes, 0)
                    randomAccessFile.write(dataLenBytes)

                    // 更新音频数据大小（偏移量40处）
                    randomAccessFile.seek(40)
                    val audioLenBytes = ByteArray(4)
                    writeIntToByteArray(totalAudioLength, audioLenBytes, 0)
                    randomAccessFile.write(audioLenBytes)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}