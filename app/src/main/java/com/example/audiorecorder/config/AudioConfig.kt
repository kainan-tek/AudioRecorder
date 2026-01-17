package com.example.audiorecorder.config

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 音频录音配置数据类
 * 支持从JSON文件加载和管理录音参数
 */
data class AudioConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 48000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferMultiplier: Int = 4,
    val outputFilePath: String = "/data/recorded_audio.wav",
    val minBufferSize: Int = 960,
    val description: String = "默认录音配置"
) {
    
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
                    ?: loadFromAssets(context).also { configs ->
                        if (configs.isNotEmpty()) createExternalConfigFile(configs)
                    }
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
                    val content = FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
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
            return AudioConfig(
                audioSource = parseAudioSource(json.optString("audioSource", "MIC")),
                sampleRate = json.optInt("sampleRate", 48000),
                channelConfig = parseChannelConfig(json.optString("channelConfig", "STEREO")),
                audioFormat = parseAudioFormat(json.optString("audioFormat", "PCM_16BIT")),
                bufferMultiplier = json.optInt("bufferMultiplier", 4),
                outputFilePath = json.optString("outputFilePath", "/data/recorded_audio.wav"),
                minBufferSize = json.optInt("minBufferSize", 960),
                description = json.optString("description", "自定义配置")
            )
        }

        private fun createExternalConfigFile(configs: List<AudioConfig>) {
            try {
                val jsonObject = JSONObject().apply {
                    put("configs", JSONArray().apply {
                        configs.forEach { config ->
                            put(JSONObject().apply {
                                put("audioSource", getAudioSourceString(config.audioSource))
                                put("sampleRate", config.sampleRate)
                                put("channelConfig", getChannelConfigString(config.channelConfig))
                                put("audioFormat", getAudioFormatString(config.audioFormat))
                                put("bufferMultiplier", config.bufferMultiplier)
                                put("outputFilePath", config.outputFilePath)
                                put("minBufferSize", config.minBufferSize)
                                put("description", config.description)
                            })
                        }
                    })
                }

                FileOutputStream(File(CONFIG_FILE_PATH)).use {
                    it.write(jsonObject.toString(2).toByteArray(Charsets.UTF_8))
                }
                Log.i(TAG, "外部配置文件已创建: $CONFIG_FILE_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "创建外部配置文件失败", e)
            }
        }

        // 解析和转换方法
        private fun parseAudioSource(source: String) = when (source.uppercase()) {
            "DEFAULT" -> MediaRecorder.AudioSource.DEFAULT
            "MIC" -> MediaRecorder.AudioSource.MIC
            "VOICE_COMMUNICATION" -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
            "VOICE_RECOGNITION" -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            "CAMCORDER" -> MediaRecorder.AudioSource.CAMCORDER
            "UNPROCESSED" -> MediaRecorder.AudioSource.UNPROCESSED
            "VOICE_PERFORMANCE" -> MediaRecorder.AudioSource.VOICE_PERFORMANCE
            else -> MediaRecorder.AudioSource.MIC
        }

        private fun parseChannelConfig(config: String) = when (config.uppercase()) {
            "MONO" -> AudioFormat.CHANNEL_IN_MONO
            "STEREO" -> AudioFormat.CHANNEL_IN_STEREO
            else -> AudioFormat.CHANNEL_IN_STEREO
        }

        private fun parseAudioFormat(format: String) = when (format.uppercase()) {
            "PCM_8BIT" -> AudioFormat.ENCODING_PCM_8BIT
            "PCM_16BIT" -> AudioFormat.ENCODING_PCM_16BIT
            "PCM_24BIT" -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            "PCM_32BIT" -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }

        private fun getAudioSourceString(source: Int) = when (source) {
            MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
            MediaRecorder.AudioSource.MIC -> "MIC"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
            MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
            MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
            MediaRecorder.AudioSource.VOICE_PERFORMANCE -> "VOICE_PERFORMANCE"
            else -> "MIC"
        }

        private fun getChannelConfigString(config: Int) = when (config) {
            AudioFormat.CHANNEL_IN_MONO -> "MONO"
            AudioFormat.CHANNEL_IN_STEREO -> "STEREO"
            else -> "STEREO"
        }

        private fun getAudioFormatString(format: Int) = when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> "PCM_8BIT"
            AudioFormat.ENCODING_PCM_16BIT -> "PCM_16BIT"
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM_24BIT"
            AudioFormat.ENCODING_PCM_32BIT -> "PCM_32BIT"
            else -> "PCM_16BIT"
        }
    }
}