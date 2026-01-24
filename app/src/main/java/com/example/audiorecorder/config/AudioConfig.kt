package com.example.audiorecorder.config

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Log
import com.example.audiorecorder.utils.AudioConstants
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
    val audioFilePath: String = AudioConstants.DEFAULT_AUDIO_FILE,
    val minBufferSize: Int = 960,
    val description: String = "Default recording configuration"
) {
    // Generate channelMask based on channel count
    val channelMask: Int
        get() = when (channelCount) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            // 4 -> AudioFormat.CHANNEL_IN_2POINT0POINT2
            // 6 -> AudioFormat.CHANNEL_IN_5POINT1
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
        private const val CONFIG_FILE_PATH = AudioConstants.EXTERNAL_CONFIG_PATH
        private const val ASSETS_CONFIG_FILE = AudioConstants.ASSETS_CONFIG_FILE

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
                minBufferSize = json.optInt("minBufferSize", 960),
                description = json.optString("description", "Custom configuration")
            )
        }

        // Parsing and conversion methods
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