package com.example.audiorecorder.config

import android.content.Context
import android.util.Log
import com.example.audiorecorder.common.AudioConstants
import org.json.JSONObject
import java.io.File

/**
 * Audio recording configuration data class
 * Supports loading and managing recording parameters from JSON files, supports up to 16-channel recording
 */
data class AudioConfig(
    val audioSource: String = "MIC",
    val sampleRate: Int = 48000,
    val channelCount: Int = 2, // Channel count (1-16)
    val audioFormatBit: Int = 16, // Bit depth: 8, 16, 24, 32
    val bufferMultiplier: Int = 2,
    val audioFilePath: String = AudioConstants.DEFAULT_AUDIO_FILE,
    val description: String = "Default recording configuration"
) {
    companion object {
        private const val TAG = "AudioConfig"
        
        /**
         * Load configuration file
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                val externalFile = File(AudioConstants.CONFIG_FILE_PATH)
                val jsonString = if (externalFile.exists()) {
                    Log.i(TAG, "Loading configuration from external file")
                    externalFile.readText()
                } else {
                    Log.i(TAG, "Loading configuration from assets")
                    context.assets.open(AudioConstants.ASSETS_CONFIG_FILE).bufferedReader().use { it.readText() }
                }
                parseConfigs(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configurations", e)
                getDefaultConfigs()
            }
        }

        /**
         * Reload configurations
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "Reloading configuration file")
            return loadConfigs(context)
        }

        private fun parseConfigs(jsonString: String): List<AudioConfig> {
            val configsArray = JSONObject(jsonString).getJSONArray("configs")
            return (0 until configsArray.length()).map { i ->
                val config = configsArray.getJSONObject(i)
                AudioConfig(
                    audioSource = config.optString("audioSource", "MIC"),
                    sampleRate = config.optInt("sampleRate", 48000),
                    channelCount = config.optInt("channelCount", 2),
                    audioFormatBit = config.optInt("audioFormat", 16),
                    bufferMultiplier = config.optInt("bufferMultiplier", 2),
                    audioFilePath = config.optString("audioFilePath", AudioConstants.DEFAULT_AUDIO_FILE),
                    description = config.optString("description", "Custom configuration")
                )
            }
        }

        private fun getDefaultConfigs(): List<AudioConfig> {
            Log.w(TAG, "Using hardcoded emergency configuration")
            return listOf(
                AudioConfig(
                    audioSource = "MIC",
                    sampleRate = 48000,
                    channelCount = 2,
                    audioFormatBit = 16,
                    bufferMultiplier = 2,
                    audioFilePath = AudioConstants.DEFAULT_AUDIO_FILE,
                    description = "Emergency Fallback - Stereo Recording"
                )
            )
        }
    }
}