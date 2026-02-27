package com.example.audiorecorder.common

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Unified audio constants definition
 * For constants and utility class management in AudioRecorder project
 */
object AudioConstants {

    // ============ File Paths ============
    const val CONFIG_FILE_PATH = "/data/audio_recorder_configs.json"
    const val ASSETS_CONFIG_FILE = "audio_recorder_configs.json"

    // ============ AudioRecord Audio Source Constants ============
    object AudioSource {
        const val DEFAULT = MediaRecorder.AudioSource.DEFAULT
        const val MIC = MediaRecorder.AudioSource.MIC
        const val VOICE_UPLINK = MediaRecorder.AudioSource.VOICE_UPLINK
        const val VOICE_DOWNLINK = MediaRecorder.AudioSource.VOICE_DOWNLINK
        const val VOICE_CALL = MediaRecorder.AudioSource.VOICE_CALL
        const val CAMCORDER = MediaRecorder.AudioSource.CAMCORDER
        const val VOICE_RECOGNITION = MediaRecorder.AudioSource.VOICE_RECOGNITION
        const val VOICE_COMMUNICATION = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        const val REMOTE_SUBMIX = MediaRecorder.AudioSource.REMOTE_SUBMIX
        const val UNPROCESSED = MediaRecorder.AudioSource.UNPROCESSED
        const val VOICE_PERFORMANCE = MediaRecorder.AudioSource.VOICE_PERFORMANCE

        // System-level audio sources
        const val ECHO_REFERENCE = 1997
        const val RADIO_TUNER = 1998
        const val HOTWORD = 1999
        const val ULTRASOUND = 2000

        // Audio source mapping table
        val MAP = mapOf(
            DEFAULT to "DEFAULT",
            MIC to "MIC",
            VOICE_UPLINK to "VOICE_UPLINK",
            VOICE_DOWNLINK to "VOICE_DOWNLINK",
            VOICE_CALL to "VOICE_CALL",
            CAMCORDER to "CAMCORDER",
            VOICE_RECOGNITION to "VOICE_RECOGNITION",
            VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
            REMOTE_SUBMIX to "REMOTE_SUBMIX",
            UNPROCESSED to "UNPROCESSED",
            VOICE_PERFORMANCE to "VOICE_PERFORMANCE",
            ECHO_REFERENCE to "ECHO_REFERENCE",
            RADIO_TUNER to "RADIO_TUNER",
            HOTWORD to "HOTWORD",
            ULTRASOUND to "ULTRASOUND"
        )
    }

    // ============ Utility Functions ============
    /**
     * Get audio source integer value from string.
     */
    fun getAudioSource(audioSource: String): Int =
        parseEnumValue(AudioSource.MAP, audioSource, MediaRecorder.AudioSource.MIC, "AudioSource")

    /**
     * Generic enum value parser with error handling.
     * Keeps the same behavior style as AudioPlayer.AudioConstants.
     */
    @Suppress("SameParameterValue")
    private fun parseEnumValue(
        map: Map<Int, String>,
        value: String,
        default: Int,
        typeName: String = "",
    ): Int {
        val result = map.entries.find { it.value == value.uppercase() }?.key ?: default
        if (result == default && value.isNotEmpty()) {
            android.util.Log.w("AudioConstants", "Unknown $typeName value: $value, using default")
        }
        return result
    }

    /**
     * Get audio format from bit depth, supporting multiple bit depths.
     */
    fun getFormatFromBitDepth(bitsPerSample: Int): Int {
        val audioFormats = mapOf(
            8 to AudioFormat.ENCODING_PCM_8BIT,
            16 to AudioFormat.ENCODING_PCM_16BIT,
            24 to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            32 to AudioFormat.ENCODING_PCM_32BIT
        )

        return audioFormats[bitsPerSample] ?: run {
            android.util.Log.w(
                "AudioConstants", "Unsupported bit depth: $bitsPerSample, using 16-bit"
            )
            AudioFormat.ENCODING_PCM_16BIT
        }
    }

    /**
     * Generate input channel mask based on channel count.
     * Uses a predefined mapping table similar to AudioPlayer.AudioConstants.
     * For unsupported counts, falls back to stereo and logs a warning.
     */
    fun getChannelMask(channelCount: Int): Int {
        val channelMasks = mapOf(
            1 to AudioFormat.CHANNEL_IN_MONO, 2 to AudioFormat.CHANNEL_IN_STEREO,
            // Requires underlying software support
            8 to 1020,     // 0x3FC
            10 to 4092,    // 0xFFC
            12 to 16380,   // 0x3FFC
            14 to 1048572, // 0xFFFFC
            16 to 4194300  // 0X3FFFFC
        )

        return channelMasks[channelCount] ?: run {
            android.util.Log.w(
                "AudioConstants",
                "Unsupported input channel count: $channelCount, using CHANNEL_IN_STEREO"
            )
            AudioFormat.CHANNEL_IN_STEREO
        }
    }
}