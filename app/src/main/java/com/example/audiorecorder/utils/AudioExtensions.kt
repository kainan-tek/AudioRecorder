package com.example.audiorecorder.utils

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Extension functions for audio-related enums and values
 */

fun Int.audioSourceToString(): String = when (this) {
    MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
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
    1997 -> "ECHO_REFERENCE"
    1998 -> "RADIO_TUNER"
    1999 -> "HOTWORD"
    2000 -> "ULTRASOUND"
    else -> "UNKNOWN($this)"
}

fun Int.audioFormatToString(): String = when (this) {
    AudioFormat.ENCODING_PCM_8BIT -> "8-bit"
    AudioFormat.ENCODING_PCM_16BIT -> "16-bit"
    AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24-bit"
    AudioFormat.ENCODING_PCM_32BIT -> "32-bit"
    AudioFormat.ENCODING_PCM_FLOAT -> "Float"
    else -> "UNKNOWN($this)"
}

fun Int.channelCountToString(): String = when (this) {
    1 -> "Mono"
    2 -> "Stereo"
    4 -> "Quad"
    6 -> "5.1"
    8 -> "7.1"
    10 -> "5.1.4"
    12 -> "7.1.4"
    else -> "${this}ch"
}