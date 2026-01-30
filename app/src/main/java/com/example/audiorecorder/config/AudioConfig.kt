package com.example.audiorecorder.config

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Audio recording configuration data class
 * Supports loading and managing recording parameters from JSON files, supports up to 16-channel recording
 */
data class AudioConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 48000,
    val channelCount: Int = 2, // Channel count (1-16)
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferMultiplier: Int = 4,
    val audioFilePath: String = "/data/recorded_audio.wav",
    val description: String = "Default recording configuration"
) {
    // Generate channelMask based on channel count
    val channelMask: Int
        get() = when (channelCount) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            in 3..16 -> {
                // For other multi-channel configurations, use bitmask to build channel configuration
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
        private const val CONFIG_FILE_PATH = "/data/audio_recorder_configs.json"
        private const val ASSETS_CONFIG_FILE = "audio_recorder_configs.json"

        // Constant mapping tables to avoid repetitive when expressions
        private val AUDIO_SOURCE_MAP = mapOf(
            MediaRecorder.AudioSource.DEFAULT to "DEFAULT",
            MediaRecorder.AudioSource.MIC to "MIC",
            MediaRecorder.AudioSource.VOICE_UPLINK to "VOICE_UPLINK",
            MediaRecorder.AudioSource.VOICE_DOWNLINK to "VOICE_DOWNLINK",
            MediaRecorder.AudioSource.VOICE_CALL to "VOICE_CALL",
            MediaRecorder.AudioSource.CAMCORDER to "CAMCORDER",
            MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
            MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
            MediaRecorder.AudioSource.REMOTE_SUBMIX to "REMOTE_SUBMIX",
            MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED",
            MediaRecorder.AudioSource.VOICE_PERFORMANCE to "VOICE_PERFORMANCE",
            1997 to "ECHO_REFERENCE",
            1998 to "RADIO_TUNER",
            1999 to "HOTWORD",
            2000 to "ULTRASOUND"
        )
        /**
         * Load configuration file
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                loadFromExternalFile().takeIf { it.isNotEmpty() } 
                    ?: loadFromAssets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configurations", e)
                emptyList()
            }
        }

        /**
         * Reload configurations
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "Reloading configuration file")
            return loadConfigs(context)
        }

        private fun loadFromExternalFile(): List<AudioConfig> {
            val file = File(CONFIG_FILE_PATH)
            return if (file.exists()) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    parseJsonConfigs(content)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read external configuration file", e)
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
                Log.e(TAG, "Failed to load configuration from assets", e)
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
                    Log.i(TAG, "Successfully parsed ${it.size} configurations")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON configuration", e)
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
                description = json.optString("description", "Custom configuration")
            )
        }

        // Parsing methods - unified approach with improved error handling
        private fun parseAudioSource(source: String): Int {
            val result = AUDIO_SOURCE_MAP.entries.find { it.value == source.uppercase() }?.key ?: MediaRecorder.AudioSource.MIC
            if (result == MediaRecorder.AudioSource.MIC && source.isNotEmpty() && source.uppercase() != "MIC") {
                Log.w(TAG, "Unknown AudioSource value: $source, using MIC")
            }
            return result
        }

        private fun parseAudioFormatFromBits(bits: Int): Int = when (bits) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> {
                Log.w(TAG, "Unknown audio format bits: $bits, using 16-bit")
                AudioFormat.ENCODING_PCM_16BIT
            }
        }
    }
}