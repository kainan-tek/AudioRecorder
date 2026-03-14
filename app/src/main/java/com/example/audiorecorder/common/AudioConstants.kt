package com.example.audiorecorder.common

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Common audio constants and utilities
 */
object AudioConstants {

    // Configuration file paths
    const val CONFIG_FILE_PATH = "/data/audio_recorder_configs.json"
    const val ASSETS_CONFIG_FILE = "audio_recorder_configs.json"

    /**
     * Error type prefixes for consistent error handling
     */
    object ErrorTypes {
        const val FILE = "[FILE]"
        const val STREAM = "[STREAM]"
        const val PERMISSION = "[PERMISSION]"
        const val PARAM = "[PARAM]"
    }

    /**
     * AudioRecord audio source constants mapping
     */
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

        // System-level audio sources (require system signature or special permissions)
        const val ECHO_REFERENCE = 1997  // Requires RECORD_AUDIO and system permissions
        const val RADIO_TUNER = 1998    // Requires system signature
        const val HOTWORD = 1999        // Requires system signature, for hotword detection
        const val ULTRASOUND = 2000     // Requires RECORD_AUDIO and system permissions

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

    /**
     * Get audio source integer value from string
     */
    fun getAudioSource(audioSource: String): Int =
        parseEnumValue(AudioSource.MAP, audioSource, MediaRecorder.AudioSource.MIC, "AudioSource")

    /**
     * Generic enum value parser with error handling
     */
    @Suppress("SameParameterValue")
    private fun parseEnumValue(
        map: Map<Int, String>,
        value: String,
        default: Int,
        typeName: String = "",
    ): Int {
        val entry = map.entries.find { it.value == value }
        if (entry != null) {
            return entry.key
        }

        if (value.isNotEmpty()) {
            android.util.Log.w(
                "AudioConstants", "Unknown $typeName value: $value, using default: $default"
            )
        }
        return default
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
            // Multi-channel input masks (requires device support)
            // These values represent specific channel configurations for professional audio recording
            8 to 0x3FC,      // 8-channel: 6 mic + 2 reference (for active noise cancellation)
            10 to 0xFFC,     // 10-channel: 5.1.4 surround sound recording
            12 to 0x3FFC,    // 12-channel: 7.1.4 surround sound recording
            14 to 0xFFFFC,   // 14-channel: extended surround configuration
            16 to 0x3FFFFC   // 16-channel: full channel configuration
        )

        return channelMasks[channelCount] ?: run {
            android.util.Log.w(
                "AudioConstants",
                "Unsupported input channel count: $channelCount, using CHANNEL_IN_STEREO"
            )
            AudioFormat.CHANNEL_IN_STEREO
        }
    }

    fun isValidSampleRate(rate: Int): Boolean = rate in 8000..192000

    fun isValidChannelCount(count: Int): Boolean = count in 1..16

    fun isValidBitDepth(depth: Int): Boolean = depth in listOf(8, 16, 24, 32)
}