package com.example.audiorecorder.utils

/**
 * Audio-related constants
 */
object AudioConstants {
    // Configuration file paths
    const val EXTERNAL_CONFIG_PATH = "/data/audio_recorder_configs.json"
    const val ASSETS_CONFIG_FILE = "audio_recorder_configs.json"
    
    // Default recording file path
    const val DEFAULT_AUDIO_FILE = "/data/recorded_audio.wav"
    
    // Progress log interval (bytes)
    const val PROGRESS_LOG_INTERVAL = 10 * 1024 * 1024L  // 10MB
    
    // Buffer size configuration
    const val BUFFER_SIZE_12CH = 8192
    const val BUFFER_SIZE_8CH = 6144
    const val BUFFER_SIZE_6CH = 4096
    const val BUFFER_SIZE_DEFAULT = 4096
}