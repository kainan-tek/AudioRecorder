package com.example.audiorecorder.config

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 音频录音配置数据类
 * 支持从JSON文件加载和管理录音参数，支持最大16声道录音
 */
data class AudioConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 48000,
    val channelCount: Int = 2, // 声道数量 (1-16)
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferMultiplier: Int = 4,
    val audioFilePath: String = "/data/recorded_audio.wav",
    val minBufferSize: Int = 960,
    val description: String = "默认录音配置"
) {
    // 根据声道数生成channelMask
    val channelMask: Int
        get() = when (channelCount) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            // 4 -> AudioFormat.CHANNEL_IN_2POINT0POINT2
            // 6 -> AudioFormat.CHANNEL_IN_5POINT1
            in 3..16 -> {
                // 对于其他多声道，使用位掩码构建声道配置
                var mask = 0
                for (i in 0 until channelCount) {
                    mask = mask or (1 shl i)
                }
                mask
            }
            else -> AudioFormat.CHANNEL_IN_STEREO
        }
    
    companion object {
        private const val TAG = "AudioConfig"
        private const val CONFIG_FILE_PATH = "/data/audio_configs.json"
        private const val ASSETS_CONFIG_FILE = "audio_configs.json"

        /**
         * 加载配置文件
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                loadFromExternalFile().takeIf { it.isNotEmpty() } 
                    ?: loadFromAssets(context)
            } catch (e: Exception) {
                Log.e(TAG, "加载配置失败", e)
                emptyList()
            }
        }

        /**
         * 重新加载配置
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "重新加载配置文件")
            return loadConfigs(context)
        }

        private fun loadFromExternalFile(): List<AudioConfig> {
            val file = File(CONFIG_FILE_PATH)
            return if (file.exists()) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    parseJsonConfigs(content)
                } catch (e: Exception) {
                    Log.e(TAG, "读取外部配置文件失败", e)
                    emptyList()
                }
            } else emptyList()
        }

        private fun loadFromAssets(context: Context): List<AudioConfig> {
            return try {
                val content = context.assets.open(ASSETS_CONFIG_FILE).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                parseJsonConfigs(content)
            } catch (e: Exception) {
                Log.e(TAG, "从assets加载配置失败", e)
                emptyList()
            }
        }

        private fun parseJsonConfigs(jsonContent: String): List<AudioConfig> {
            return try {
                val jsonObject = JSONObject(jsonContent)
                val configsArray = jsonObject.getJSONArray("configs")
                
                (0 until configsArray.length()).map { i ->
                    parseAudioConfig(configsArray.getJSONObject(i))
                }.also {
                    Log.i(TAG, "成功解析 ${it.size} 个配置")
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析JSON配置失败", e)
                emptyList()
            }
        }

        private fun parseAudioConfig(json: JSONObject): AudioConfig {
            val channelCount = json.optInt("channelCount", 2)
            val audioFormatBits = json.optInt("audioFormat", 16)
            
            return AudioConfig(
                audioSource = parseAudioSource(json.optString("audioSource", "MIC")),
                sampleRate = json.optInt("sampleRate", 48000),
                channelCount = channelCount,
                audioFormat = parseAudioFormatFromBits(audioFormatBits),
                bufferMultiplier = json.optInt("bufferMultiplier", 4),
                audioFilePath = json.optString("audioFilePath", "/data/recorded_audio.wav"),
                minBufferSize = json.optInt("minBufferSize", 960),
                description = json.optString("description", "自定义配置")
            )
        }

        // 解析和转换方法
        private fun parseAudioSource(source: String) = when (source.uppercase()) {
            "DEFAULT" -> MediaRecorder.AudioSource.DEFAULT
            "MIC" -> MediaRecorder.AudioSource.MIC
            "VOICE_UPLINK" -> MediaRecorder.AudioSource.VOICE_UPLINK
            "VOICE_DOWNLINK" -> MediaRecorder.AudioSource.VOICE_DOWNLINK
            "VOICE_CALL" -> MediaRecorder.AudioSource.VOICE_CALL
            "CAMCORDER" -> MediaRecorder.AudioSource.CAMCORDER
            "VOICE_RECOGNITION" -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            "VOICE_COMMUNICATION" -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
            "REMOTE_SUBMIX" -> MediaRecorder.AudioSource.REMOTE_SUBMIX
            "UNPROCESSED" -> MediaRecorder.AudioSource.UNPROCESSED
            "VOICE_PERFORMANCE" -> MediaRecorder.AudioSource.VOICE_PERFORMANCE
            "ECHO_REFERENCE" -> 1997 // MediaRecorder.AudioSource.ECHO_REFERENCE
            "RADIO_TUNER" -> 1998 // MediaRecorder.AudioSource.RADIO_TUNER
            "HOTWORD" -> 1999 // MediaRecorder.AudioSource.HOTWORD
            "ULTRASOUND" -> 2000 // MediaRecorder.AudioSource.ULTRASOUND
            else -> MediaRecorder.AudioSource.MIC
        }

        private fun parseAudioFormatFromBits(bits: Int) = when (bits) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
    }
}