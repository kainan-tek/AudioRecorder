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

    const val DEFAULT_AUDIO_FILE = "/data/recorded_48k_2ch_16bit.wav"
    
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
     * Get audio source integer value from string
     */
    fun getAudioSource(audioSource: String): Int {
        val result = AudioSource.MAP.entries.find { it.value == audioSource.uppercase() }?.key ?: MediaRecorder.AudioSource.MIC
        if (result == MediaRecorder.AudioSource.MIC && audioSource.isNotEmpty() && audioSource.uppercase() != "MIC") {
            android.util.Log.w("AudioConstants", "Unknown AudioSource value: $audioSource, using MIC")
        }
        return result
    }

    /**
     * Get audio format from bit depth, supporting multiple bit depths
     */
    fun getAudioFormat(bitsPerSample: Int): Int {
        val audioFormats = mapOf(
            8 to AudioFormat.ENCODING_PCM_8BIT,
            16 to AudioFormat.ENCODING_PCM_16BIT,
            24 to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            32 to AudioFormat.ENCODING_PCM_32BIT
        )

        return audioFormats[bitsPerSample] ?: run {
            android.util.Log.w("AudioConstants", "Unsupported bit depth: $bitsPerSample, using 16-bit")
            AudioFormat.ENCODING_PCM_16BIT
        }
    }

    /**
     * Generate channel mask based on channel count
     */
    fun getChannelMask(channelCount: Int): Int {
        return when (channelCount) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            in 3..16 -> {
                // For other multi-channel configurations, use bit mask to build channel configuration
                var mask = 0
                for (i in 0 until channelCount) {
                    mask = mask or (1 shl i)
                }
                mask
            }
            else -> AudioFormat.CHANNEL_IN_STEREO
        }
    }
}