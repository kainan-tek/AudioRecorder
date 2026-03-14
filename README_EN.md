# AudioRecorder

[中文文档](README.md) | English

An audio recorder based on Android AudioRecord API, supporting multiple recording configurations and
WAV file output.

## Table of Contents

- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Introduction

AudioRecorder is an Android audio recorder based on Android AudioRecord API, supporting multiple
recording scenarios and WAV file output.

### Key Features

- **15 Recording Scenarios**: Default, microphone, voice call, voice recognition, camcorder, stereo
  recording, echo reference, voice performance, remote submix, unprocessed, radio tuner, hotword,
  ultrasound, etc.
- **Complete Audio Support**: 1-16 channels, 8kHz-192kHz sample rates, 8/16/24/32-bit PCM
- **WAV File Output**: Automatic WAV header generation with smart file naming
- **Flexible Configuration**: JSON configuration file with external hot-reload support

### Recording Scenarios

| Scenario            | Audio Source        | Sample Rate | Channels | Typical Use                 |
|---------------------|---------------------|-------------|----------|-----------------------------|
| Default             | DEFAULT             | 48kHz       | Stereo   | Default recording           |
| Microphone          | MIC                 | 48kHz       | Stereo   | Standard recording          |
| Voice Uplink        | VOICE_UPLINK        | 16kHz       | Mono     | Voice call uplink           |
| Voice Downlink      | VOICE_DOWNLINK      | 16kHz       | Mono     | Voice call downlink         |
| Voice Call          | VOICE_CALL          | 16kHz       | Mono     | Bidirectional voice call    |
| Camcorder           | CAMCORDER           | 48kHz       | Stereo   | Video recording             |
| Voice Recognition   | VOICE_RECOGNITION   | 16kHz       | Mono     | ASR applications            |
| Voice Communication | VOICE_COMMUNICATION | 16kHz       | Mono     | VoIP applications           |
| Remote Submix       | REMOTE_SUBMIX       | 48kHz       | Stereo   | Remote submix               |
| Unprocessed         | UNPROCESSED         | 48kHz       | Stereo   | Unprocessed recording       |
| Voice Performance   | VOICE_PERFORMANCE   | 48kHz       | Mono     | Professional recording      |
| Echo Reference      | ECHO_REFERENCE      | 48kHz       | Stereo   | AEC reference               |
| Radio Tuner         | RADIO_TUNER         | 48kHz       | Stereo   | Radio recording             |
| Hotword             | HOTWORD             | 16kHz       | Mono     | Low-power hotword detection |
| Ultrasound          | ULTRASOUND          | 48kHz       | Mono     | Ultrasound recording        |

## Quick Start

### Basic Usage

1. **Select Config** - Choose recording scenario via dropdown menu
2. **Start Recording** - Tap green record button
3. **Stop Recording** - Tap red stop button
4. **Reload Config** - Long-press dropdown to reload external config

### Common Operations

```bash
# View recording logs
adb logcat -s AudioRecorder MainActivity AudioConfig

# Check config file
adb shell cat /data/audio_recorder_configs.json

# View recording files
adb shell ls -la /data/recorded_*.wav
```

## Installation

### Requirements

- **Android Version**: Android 12L (API 32) or higher
- **Development Environment**: Android Studio
- **Build System**: Gradle

### Build and Install

```bash
git clone https://github.com/kainan-tek/AudioRecorder.git
cd AudioRecorder
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Permissions

| Permission               | Purpose           | Version     |
|--------------------------|-------------------|-------------|
| `RECORD_AUDIO`           | Recording         | All         |
| `READ_EXTERNAL_STORAGE`  | Read config files | Android 12- |
| `WRITE_EXTERNAL_STORAGE` | Save recordings   | Android 9-  |

```bash
# Grant recording permission manually
adb shell pm grant com.example.audiorecorder android.permission.RECORD_AUDIO
```

## Configuration

### Config File Location

- **External Config**: `/data/audio_recorder_configs.json` (loaded first)
- **Built-in Config**: `app/src/main/assets/audio_recorder_configs.json`

### Config File Format

```json
{
  "configs": [
    {
      "audioSource": "MIC",
      "sampleRate": 48000,
      "channelCount": 2,
      "audioFormat": 16,
      "bufferMultiplier": 2,
      "audioFilePath": "/data/recorded_48k_2ch_16bit.wav",
      "description": "Microphone Recording (48kHz Stereo)"
    }
  ]
}
```

### Configuration Parameters

#### Audio Source

| Value                 | Description                | Typical Use                  |
|-----------------------|----------------------------|------------------------------|
| `DEFAULT`             | Default                    | Default recording            |
| `MIC`                 | Microphone                 | Standard recording           |
| `VOICE_UPLINK`        | Voice uplink               | Call recording uplink        |
| `VOICE_DOWNLINK`      | Voice downlink             | Call recording downlink      |
| `VOICE_CALL`          | Voice call (bidirectional) | Bidirectional call recording |
| `CAMCORDER`           | Camcorder                  | Video recording              |
| `VOICE_RECOGNITION`   | Voice recognition          | ASR applications             |
| `VOICE_COMMUNICATION` | Voice communication        | VoIP applications            |
| `REMOTE_SUBMIX`       | Remote submix              | Remote submix recording      |
| `UNPROCESSED`         | Unprocessed                | Unprocessed recording        |
| `VOICE_PERFORMANCE`   | Voice performance          | Professional recording       |
| `ECHO_REFERENCE`      | Echo reference             | AEC reference                |
| `RADIO_TUNER`         | Radio tuner                | Radio recording              |
| `HOTWORD`             | Hotword                    | Low-power hotword detection  |
| `ULTRASOUND`          | Ultrasound                 | Ultrasound recording         |

#### Sample Rate

| Value | Description | Typical Use            |
|-------|-------------|------------------------|
| 8000  | 8kHz        | Telephone quality      |
| 16000 | 16kHz       | Voice recognition      |
| 44100 | 44.1kHz     | CD quality             |
| 48000 | 48kHz       | Professional recording |

#### Bit Depth

| Value | Description              |
|-------|--------------------------|
| 8     | 8-bit PCM                |
| 16    | 16-bit PCM (recommended) |
| 24    | 24-bit PCM               |
| 32    | 32-bit PCM               |

### Smart File Naming

When `audioFilePath` is empty, auto-generated filename format:

```
rec_YYYYMMDD_HHMMSS_mmm_[sampleRate]k_[channels]ch_[bitDepth]bit.wav
```

**Example**: `rec_20240124_143052_123_48k_1ch_16bit.wav`

## API Reference

### AudioRecorder Class

```kotlin
class AudioRecorder(private val context: Context) {
    fun setAudioConfig(config: AudioConfig)      // Set audio configuration
    fun startRecording(): Boolean                // Start recording
    fun stopRecording()                          // Stop recording (idempotent)
    fun isRecording(): Boolean                   // Check recording status
    fun release()                                // Release resources
    fun setRecordingListener(listener: RecordingListener?)  // Set listener
}
```

### RecordingListener Interface

```kotlin
interface RecordingListener {
    fun onRecordingStarted()                     // Recording started callback
    fun onRecordingStopped()                     // Recording stopped callback
    fun onRecordingError(error: String)          // Recording error callback
}
```

### Error Prefixes

| Prefix         | Description                |
|----------------|----------------------------|
| `[FILE]`       | File operation error       |
| `[STREAM]`     | Audio stream error         |
| `[PERMISSION]` | Permission error           |
| `[PARAM]`      | Parameter validation error |

## Troubleshooting

### Common Issues

#### 1. Recording Failed

```bash
# Check recording permission
adb shell dumpsys package com.example.audiorecorder | grep RECORD_AUDIO

# View detailed logs
adb logcat -s AudioRecorder
```

#### 2. Permission Issues

```bash
adb shell pm grant com.example.audiorecorder android.permission.RECORD_AUDIO
adb shell setenforce 0
```

#### 3. File Save Failed

```bash
adb shell df /data          # Check disk space
adb shell ls -la /data/     # Check file permissions
```

### Debug Commands

```bash
adb logcat -s AudioRecorder MainActivity AudioConfig
adb logcat -s AudioRecord AudioFlinger
```

## Related Projects

- [AAudioPlayer](https://github.com/kainan-tek/AAudioPlayer) - High-performance player based on
  AAudio API
- [AAudioRecorder](https://github.com/kainan-tek/AAudioRecorder) - High-performance recorder based
  on AAudio API
- [AudioPlayer](https://github.com/kainan-tek/AudioPlayer) - Audio player based on AudioTrack API
- [audio_test_client](https://github.com/kainan-tek/audio_test_client) - Android system-level audio
  testing tool

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**Note**: This project is for learning and testing purposes only. Please comply with relevant
recording laws and regulations.
