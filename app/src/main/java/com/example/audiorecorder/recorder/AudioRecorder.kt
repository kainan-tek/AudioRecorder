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

enum class RecorderState { IDLE, RECORDING, ERROR }

/**
 * 音频录音器核心类
 * 支持配置化录音参数
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
            Log.w(TAG, "录音中无法更改配置")
            return
        }
        currentConfig = config
        Log.i(TAG, "配置已更新: ${config.description}")
    }

    fun startRecording(): Boolean {
        // 显式检查录音权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            handleError("录音权限未授予")
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
                Log.i(TAG, "录音开始成功")
                true
            } else false
        } catch (_: SecurityException) {
            handleError("录音权限被拒绝")
            false
        } catch (e: Exception) {
            handleError("录音初始化失败: ${e.message}")
            false
        }
    }

    fun stopRecording() {
        if (!isRecording.get()) return

        isRecording.set(false)
        recordingJob?.cancel()
        releaseResources()
        listener?.onRecordingStopped()
        Log.i(TAG, "录音已停止")
    }

    fun release() {
        stopRecording()
        recordingScope.cancel()
    }

    private fun createOutputFile(): Boolean {
        val outputPath = currentConfig.outputFilePath.takeIf { it.isNotEmpty() } 
            ?: generateOutputFilePath()
        
        waveFile = WaveFile(outputPath)
        val channelCount = currentConfig.channelCount // 直接使用配置中的声道数
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        
        return if (waveFile!!.create(currentConfig.sampleRate, channelCount, bitsPerSample)) {
            Log.d(TAG, "输出文件已创建: $outputPath (${channelCount}声道)")
            true
        } else {
            handleError("无法创建输出文件: $outputPath")
            false
        }
    }

    private fun initializeAudioRecord(): Boolean {
        return try {
            if (!validateAudioParameters()) return false

            val minBufferSize = AudioRecord.getMinBufferSize(
                currentConfig.sampleRate, 
                currentConfig.channelConfig, 
                currentConfig.audioFormat
            )
            
            if (minBufferSize <= 0) {
                handleError("不支持的音频参数组合")
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
                        .setChannelMask(currentConfig.channelConfig)
                        .setEncoding(currentConfig.audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                handleError("AudioRecord初始化失败")
                return false
            }

            Log.i(TAG, "AudioRecord初始化成功 - ${currentConfig.description}")
            true
        } catch (_: SecurityException) {
            handleError("录音权限被拒绝")
            false
        } catch (e: Exception) {
            handleError("AudioRecord创建失败: ${e.message}")
            false
        }
    }

    private fun validateAudioParameters(): Boolean {
        val sampleRate = currentConfig.sampleRate
        val channelCount = currentConfig.channelCount // 直接使用配置中的声道数
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        
        return when {
            sampleRate !in 8000..192000 -> {
                handleError("不支持的采样率: ${sampleRate}Hz")
                false
            }
            channelCount !in 1..16 -> {
                handleError("不支持的声道数: $channelCount")
                false
            }
            bitsPerSample !in listOf(8, 16, 24, 32) -> {
                handleError("不支持的位深度: ${bitsPerSample}bit")
                false
            }
            else -> true
        }
    }

    private fun startRecordingLoop() {
        recordingJob = recordingScope.launch {
            val bufferSize = 4096
            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L
            
            try {
                audioRecord?.startRecording()
                Log.i(TAG, "开始录音 - ${currentConfig.description}")
                
                while (isActive && isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    
                    if (bytesRead <= 0) break
                    
                    waveFile?.writeAudioData(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    // 每10MB输出一次进度
                    if (totalBytes % (10 * 1024 * 1024) == 0L && totalBytes > 0) {
                        val mbRecorded = totalBytes / (1024.0 * 1024.0)
                        Log.d(TAG, "录音进度: ${String.format("%.1f", mbRecorded)}MB")
                    }
                }
            } catch (_: SecurityException) {
                if (isRecording.get()) {
                    handleError("录音权限被拒绝")
                }
            } catch (e: Exception) {
                if (isRecording.get()) {
                    handleError("录音过程出错: ${e.message}")
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
            Log.e(TAG, "资源释放出错", e)
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
        val channelCount = currentConfig.channelCount // 直接使用配置中的声道数
        val bitsPerSample = getBitsPerSample(currentConfig.audioFormat)
        val fileName = "recording_${currentConfig.sampleRate}Hz_${channelCount}ch_${bitsPerSample}bit_${dateTime}.wav"
        return File(context.filesDir, fileName).absolutePath
    }
}
