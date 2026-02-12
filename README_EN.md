# AudioRecorder

[中文文档](README.md) | English

A high-performance audio recording test application based on Android AudioRecord API, supporting 15 recording configurations and real-time WAV file writing.

## 📋 Overview

AudioRecorder is an audio recording test tool designed for the Android platform, using the Android AudioRecord API. This project demonstrates how to implement high-quality audio recording in Android applications, supporting various recording scenarios and audio source types. It's an ideal tool for audio developers and test engineers.

## ✨ Key Features

- **🎙️ High-Performance Recording**: Audio recording based on AudioRecord API
- **🔧 15 Recording Presets**: Covering microphone, voice call, camcorder, high-performance recording scenarios
- **📱 Modern UI**: Intuitive control interface with Material Design 3 style
- **🎵 Multi-Format Support**: PCM 8-bit, 16-bit, 24-bit, 32-bit formats
- **⚡ Real-time Processing**: Audio data written to WAV file in real-time, supports continuous recording
- **🛠️ Dynamic Configuration System**: Runtime switching of recording configurations, external JSON config file support
- **📝 Smart Naming**: Auto-generated recording filenames with timestamps
- **🏗️ MVVM Architecture**: Clear code structure and modular design
- **🔊 Multi-Channel Support**: Supports 1-16 channel recording

## 🚀 Quick Start

### System Requirements

- Android 12 (API 32) or higher
- Device with AudioRecord support
- Development Environment: Android Studio

### Permission Requirements

- `RECORD_AUDIO`: Recording permission (required for core functionality)

### Installation Steps

1. **Clone Project**
   ```bash
   git clone https://github.com/kainan-tek/AudioRecorder.git
   cd AudioRecorder
   ```

2. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Optional Config File**
   ```bash
   # Push custom config file (optional)
   adb root && adb remount && adb shell setenforce 0
   adb push audio_recorder_configs.json /data/
   ```

## 📖 Usage Guide

### Basic Operations

1. **Recording Control**
   - 🎙️ **Start Recording**: Tap the green recording button
   - ⏹️ **Stop Recording**: Tap the red stop button
   - ⚙️ **Recording Config**: Tap config button to switch recording settings

2. **Configuration Management**
   - Auto-load configurations on app startup
   - Support dynamic loading from external files
   - Switch between different recording scenarios via dropdown menu at runtime
   - Long-press config dropdown to reload external config file

### Configuration Switching Flow

1. Tap config dropdown to view all available configurations
2. Select desired recording scenario configuration
3. Configuration takes effect immediately and displays on interface
4. Start recording to test audio effect
5. To reload external config file, long-press config dropdown

## 🎙️ Audio Format Support

### Channel Configurations
| Channels | Config Name    | Description   | Use Cases                           |
|----------|----------------|---------------|-------------------------------------|
| 1        | Mono           | Mono          | Voice recording, call recording     |
| 2        | Stereo         | Stereo        | Standard recording, music recording |
| 4        | Quad           | Quad          | Professional recording, ambient     |
| 6        | 5.1 Surround   | 5.1 Surround  | Multi-channel, film production      |
| 8        | 7.1 Surround   | 7.1 Surround  | High-end equipment, professional    |
| 1-16     | Other Configs  | Auto-mapping  | Auto-select best config             |

### Audio Parameters
- **Sample Rate**: 8kHz - 192kHz (Common: 16kHz, 48kHz)
- **Bit Depth**: 8/16/24/32 bit
- **Format**: WAV (PCM)
- **Maximum Channels**: 16 channels
- **Configuration System**: Supports various audio sources and buffer configurations

## 🎙️ 15 Preset Configuration Scenarios

### Basic Audio Sources (Available for Regular Apps)
1. **Default Audio Source** - System auto-selects most suitable audio source (48kHz stereo)
2. **Microphone Recording** - General recording scenario (48kHz stereo)
3. **Camcorder Recording** - Video recording audio (48kHz stereo)
4. **Voice Recognition** - Voice recognition optimized (16kHz mono)
5. **Voice Communication** - VoIP call optimized (16kHz mono)
6. **Unprocessed Audio** - Raw signal recording (48kHz stereo)
7. **Voice Performance** - Real-time processing recording (48kHz mono)

### System-Level Audio Sources (Require Special Permissions)
8. **Voice Call Uplink** - Voice communication send (16kHz mono, requires system permission)
9. **Voice Call Downlink** - Voice communication receive (16kHz mono, requires system permission)
10. **Voice Call Bidirectional** - Complete call recording (16kHz mono, requires system permission)
11. **Remote Submix** - Internal audio stream (48kHz stereo, requires system permission)
12. **Echo Reference Signal** - For echo cancellation (48kHz stereo, requires system permission)
13. **Radio Tuner** - Radio station audio (48kHz stereo, requires system permission)
14. **Hotword Detection** - Low-priority hotword detection (16kHz mono, requires system permission)
15. **Ultrasound Recording** - Ultrasound frequency recording (48kHz mono, requires system permission)

> **Important Note**: System-level audio sources require corresponding system permissions. Regular third-party apps cannot use these audio sources.

## 🔧 Configuration File

### Configuration Location

- **External Config**: `/data/audio_recorder_configs.json` (priority)
- **Built-in Config**: `app/src/main/assets/audio_recorder_configs.json`

### Configuration Format

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
      "description": "Microphone recording configuration"
    }
  ]
}
```

### Audio File Location

- **Recommended Location**: `/data/` directory (requires root permission)
- **Default Location**: App private directory (`/data/data/com.example.audiorecorder/files/`)
- **Custom Location**: Specify full path via config file
- **Supported Format**: Standard WAV files (RIFF/WAVE format)
- **File Requirements**: 8/16/24/32-bit PCM format, 1-16 channels

### Supported Constant Values

**Audio Source:**

#### Basic Audio Sources (Available for Regular Apps)
- `DEFAULT` - Default audio source, system auto-selects
- `MIC` - Microphone audio source, preferred for standard recording apps
- `CAMCORDER` - Camera recording, optimized for video recording
- `VOICE_RECOGNITION` - Voice recognition optimized
- `VOICE_COMMUNICATION` - Voice call optimized, suitable for VoIP
- `VOICE_PERFORMANCE` - Voice performance optimized, for real-time processing
- `UNPROCESSED` - Unprocessed audio, provides raw audio signal

#### System-Level Audio Sources (Require Special Permissions)
- `VOICE_UPLINK` - Voice call uplink signal (requires CAPTURE_AUDIO_OUTPUT permission)
- `VOICE_DOWNLINK` - Voice call downlink signal (requires CAPTURE_AUDIO_OUTPUT permission)
- `VOICE_CALL` - Voice call bidirectional signal (requires CAPTURE_AUDIO_OUTPUT permission)
- `REMOTE_SUBMIX` - Remote submix (requires CAPTURE_AUDIO_OUTPUT permission)
- `ECHO_REFERENCE` - Echo reference signal (requires CAPTURE_AUDIO_OUTPUT permission)
- `RADIO_TUNER` - Radio tuner output (requires CAPTURE_TUNER_AUDIO_INPUT permission)
- `HOTWORD` - Hotword detection (requires CAPTURE_AUDIO_HOTWORD permission)
- `ULTRASOUND` - Ultrasound recording (requires ACCESS_ULTRASOUND permission)

**Audio Format:**
- `8` - 8-bit integer (basic quality, saves storage space)
- `16` - 16-bit integer (standard quality, recommended)
- `24` - 24-bit integer (high quality, professional recording)
- `32` - 32-bit integer (highest quality, large storage)

**Sample Rate:**
- `8000` - Telephone quality (voice calls)
- `16000` - Voice recognition standard (voice calls, recognition, hotword detection)
- `44100` - CD quality (standard recording)
- `48000` - Professional audio standard (recommended, most scenarios)
- `96000` - Hi-Fi audio (professional production)
- `192000` - Highest quality (ultrasound recording, special purposes)

**Channel Count:**
- `1` - Mono (saves space, voice recording)
- `2` - Stereo (standard recording, music recording)
- `4-16` - Multi-channel recording (professional use, ambient recording)

## 📝 Smart File Naming

### Auto-Naming Rules

When `audioFilePath` in configuration is empty, the system auto-generates filename:

```
recording_[sampleRate]Hz_[channels]ch_[bitDepth]bit_[timestamp].wav
```

**Example Filenames:**
- `recording_16000Hz_2ch_16bit_20240124_143052.wav`
- `recording_48000Hz_1ch_16bit_20240124_143052.wav`
- `recording_44100Hz_2ch_24bit_20240124_143052.wav`

### File Storage Location

- **Default Location**: App private directory (`/data/data/com.example.audiorecorder/files/`)
- **Custom Location**: Specify full path via config file
- **External Storage**: Can configure to `/data/` directory (requires appropriate permissions)

## 🏗️ Technical Architecture

### Core Components

- **AudioRecorder**: Kotlin-written audio recorder wrapper class with permission management
- **AudioConfig**: Recording configuration management class with dynamic config loading
- **RecorderViewModel**: MVVM architecture view model managing recording state
- **MainActivity**: Modern main interface controller with permission management and user interaction
- **WaveFile**: WAV file writer supporting real-time writing

### Technology Stack

- **Language**: Kotlin
- **Audio API**: Android AudioRecord
- **Architecture**: MVVM + LiveData
- **UI**: Material Design 3
- **Concurrency**: Kotlin Coroutines
- **Minimum Version**: Android 12 (API 32)
- **Target Version**: Android 15 (API 36)
- **Compile Version**: Android 15 (API 36)

### Dependencies

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

## 🔍 Technical Details

### AudioRecord Integration

- Audio recording using AudioRecord API
- Multiple audio format support (8/16/24/32-bit PCM)
- Complete error handling mechanism
- Real-time WAV file writing
- Smart buffer management

### Data Flow Architecture

```
Microphone → AudioRecord → Audio Buffer → WaveFile → WAV File
                                ↓
                         Kotlin Coroutine → UI State Update
```

### WAV File Writing

- **Real-time Writing**: Continuous audio data writing during recording
- **Format Support**: Standard RIFF/WAVE format
- **Multi-channel Support**: 1-16 channel recording
- **Sample Rate Range**: 8kHz - 192kHz
- **Bit Depth Support**: 8/16/24/32-bit
- **Auto File Header**: Automatically updates WAV file header after recording completes

### Permission Management

- Runtime permission requests
- Permission status checking
- User-friendly permission explanations
- Graceful handling when permissions are denied

## 📚 API Reference

### AudioRecorder Class
```kotlin
class AudioRecorder(context: Context) {
    fun setAudioConfig(config: AudioConfig)                      // Set configuration
    fun startRecording(): Boolean                                // Start recording
    fun stopRecording(): Boolean                                 // Stop recording
    fun release()                                                // Release resources
    fun setRecordingListener(listener: RecordingListener?)       // Set listener
}
```

### RecorderViewModel Class
```kotlin
class RecorderViewModel : ViewModel() {
    val recorderState: LiveData<RecorderState>                   // Recording state
    val statusMessage: LiveData<String>                          // Status message
    val errorMessage: LiveData<String?>                          // Error message
    val currentConfig: LiveData<AudioConfig?>                    // Current config
    
    fun startRecording()                                         // Start recording
    fun stopRecording()                                          // Stop recording
    fun setAudioConfig(config: AudioConfig)                      // Set configuration
    fun getAllAudioConfigs(): List<AudioConfig>                  // Get all configs
    fun reloadConfigurations()                                   // Reload configurations
}
```

### AudioConfig Class
```kotlin
data class AudioConfig(
    val audioSource: Int,                                        // Audio source
    val sampleRate: Int,                                         // Sample rate
    val channelCount: Int,                                       // Channel count
    val audioFormat: Int,                                        // Audio format
    val bufferMultiplier: Int,                                   // Buffer multiplier
    val audioFilePath: String,                                   // Audio file path
    val description: String                                      // Config description
) {
    val channelMask: Int                                         // Channel mask
    
    companion object {
        fun loadConfigs(context: Context): List<AudioConfig>     // Load configs
        fun reloadConfigs(context: Context): List<AudioConfig>   // Reload configs
    }
}
```

## 🐛 Troubleshooting

### Common Issues

1. **Recording Failure**
   - Confirm recording permission granted
   - Check device microphone functionality
   - Verify audio parameter combination support

2. **Permission Issues**
   ```bash
   # For system-level audio sources, system permissions required
   adb root && adb remount && adb shell setenforce 0
   ```

3. **Config Loading Failure**
   - Check JSON format correctness
   - Verify config file path
   - View app logs for detailed error information

4. **File Write Failure**
   - Check output directory write permissions
   - Confirm sufficient storage space
   - Verify file path format correctness

### Debug Information
```bash
# View app logs
adb logcat -s AudioRecorder MainActivity RecorderViewModel

# Check config file
adb shell cat /data/audio_recorder_configs.json

# View recording files
adb shell ls -la /data/recorded_*.wav
adb shell ls -la /data/data/com.example.audiorecorder/files/
```

### Performance Optimization Tips

1. **Voice Recording**
   - Use `VOICE_RECOGNITION` or `VOICE_COMMUNICATION` audio source
   - Select 16kHz sample rate and mono configuration
   - Use smaller buffer multiplier (2-4)

2. **High-Quality Recording**
   - Use `MIC` audio source
   - Select 48kHz or higher sample rate
   - Use stereo and high bit depth
   - Increase buffer multiplier (6-8)

3. **Real-time Recording**
   - Use `VOICE_PERFORMANCE` audio source
   - Set smaller buffer multiplier (1-2)
   - Select appropriate sample rate to balance quality and latency

## 📊 Performance Metrics

- **Sample Rate**: 8kHz - 192kHz (Common: 16kHz voice, 48kHz professional)
- **Channel Count**: 1-16 channels
- **Bit Depth**: 8/16/24/32-bit
- **Buffer**: Configurable buffer multiplier (1-8x)
- **Supported Format**: WAV (PCM)
- **Maximum Recording Duration**: Limited by device storage
- **Real-time Performance**: Supports continuous long-duration recording

## 🔗 Related Projects

- [**AudioPlayer**](https://github.com/kainan-tek/AudioPlayer) - Companion audio playback app based on AudioTrack API
- [**AAudioRecorder**](https://github.com/kainan-tek/AAudioRecorder) - High-performance recorder based on AAudio API (standalone project)
- [**AAudioPlayer**](https://github.com/kainan-tek/AAudioPlayer) - Player based on AAudio API (standalone project)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This project is for learning and testing purposes only. Please ensure use in appropriate devices and environments, and comply with relevant recording laws and regulations.
