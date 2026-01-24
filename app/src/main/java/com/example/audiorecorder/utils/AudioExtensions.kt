package com.example.audiorecorder.utils

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Audio-related extension functions for simplifying enum to string conversion
 */

fun Int.audioSourceToString(): String = when(this) {
    MediaRecorder.AudioSource.MIC -> "MIC"
    MediaRecorder.AudioSource.VOICE_UPLINK -> "VOICE_UPLINK"
    MediaRecorder.AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK"
    MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
    MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
    MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
    MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
    MediaRecorder.AudioSource.REMOTE_SUBMIX -> "REMOTE_SUBMIX"
    MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
    MediaRecorder.AudioSource.VOICE_PERFORMANCE -> "VOICE_PERFORMANCE"
    else -> "UNKNOWN"
}

fun Int.audioFormatToString(): String = when(this) {
    AudioFormat.ENCODING_PCM_8BIT -> "8bit"
    AudioFormat.ENCODING_PCM_16BIT -> "16bit"
    AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24bit"
    AudioFormat.ENCODING_PCM_32BIT -> "32bit"
    AudioFormat.ENCODING_PCM_FLOAT -> "float"
    else -> "16bit"
}

fun Int.channelCountToString(): String = when(this) {
    1 -> "Mono"
    2 -> "Stereo"
    else -> "$this Channels"
}