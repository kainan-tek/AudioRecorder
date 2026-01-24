# Audio Recorder

一个基于Android AudioRecord API的高性能音频录制器测试程序，支持10种录音配置和实时WAV文件写入。

## 📋 项目概述

Audio Recorder是一个专为Android平台设计的音频录制测试工具，使用Android AudioRecord API。该项目展示了如何在Android应用中实现高质量的音频录制，支持多种录音使用场景和音频源类型。

## ✨ 主要特性

- **🎙️ 高性能录音**: 基于AudioRecord API实现音频录制
- **🔧 10种录音预设**: 涵盖麦克风、语音通话、摄像、高性能等录音场景
- **📱 现代化界面**: Material Design风格的直观控制界面
- **🎵 多格式支持**: 支持PCM 16位、24位、32位格式
- **⚡ 实时处理**: 音频数据实时写入WAV文件，支持连续录制
- **🛠️ 动态配置**: 运行时切换录音配置，支持JSON配置文件
- **📝 智能命名**: 自动生成带时间戳的录音文件名
- **🏗️ MVVM架构**: 清晰的代码结构和模块化设计

## 🏗️ 技术架构

### 核心组件

- **AudioRecorder**: Kotlin编写的音频录制器封装类，集成权限管理
- **AudioConfig**: 录音配置管理类，支持动态加载配置
- **RecorderViewModel**: MVVM架构的视图模型，管理录音状态
- **MainActivity**: 现代化主界面控制器，提供权限管理和用户交互
- **WaveFile**: WAV文件写入器，支持实时写入

### 技术栈

- **语言**: Kotlin
- **音频API**: Android AudioRecord
- **架构**: MVVM + LiveData
- **UI**: Material Design 3
- **并发**: Kotlin Coroutines
- **最低版本**: Android 13 (API 33)
- **目标版本**: Android 15 (API 36)

## 🎙️ 支持的录音场景

### 10种预设配置

1. **麦克风录音** - 通用录音场景 (16kHz立体声)
2. **语音通话上行** - 语音通信发送 (8kHz单声道，需系统权限)
3. **语音通话下行** - 语音通信接收 (8kHz单声道，需系统权限)
4. **语音通话双向** - 完整通话录制 (8kHz单声道，需系统权限)
5. **摄像录音** - 视频录制音频 (16kHz立体声)
6. **语音识别** - 语音识别优化 (16kHz立体声)
7. **语音通信** - VoIP通话优化 (16kHz立体声)
8. **远程子混音** - 内部音频流 (16kHz立体声，需系统权限)
9. **未处理音频** - 原始信号录制 (48kHz立体声)
10. **语音性能** - 实时处理录制 (48kHz单声道)

## 🚀 快速开始

### 系统要求

- Android 13 (API 33) 或更高版本
- 支持AudioRecord的设备
- 开发环境: Android Studio

### 权限要求

- `RECORD_AUDIO`: 录音权限 (核心功能必需)
- `READ_MEDIA_AUDIO` (API 33+): 读取音频文件
- `READ_EXTERNAL_STORAGE` (API ≤32): 读取外部存储

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-repo/AudioRecorder.git
   cd AudioRecorder
   ```

2. **编译安装**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📖 使用说明

### 基本操作

1. **录音控制**
   - 🎙️ **开始录音**: 点击绿色录音按钮
   - ⏹️ **停止录音**: 点击红色停止按钮
   - ⚙️ **录音配置**: 点击配置按钮切换录音设置

2. **配置管理**
   - 应用启动时自动加载配置
   - 支持从外部文件动态加载配置
   - 可在运行时切换不同的录音场景

## 🔧 配置文件

### 配置位置

- **外部配置**: `/data/audio_recorder_configs.json` (优先)
- **内置配置**: `app/src/main/assets/audio_recorder_configs.json`

### 配置格式

```json
{
  "configs": [
    {
      "audioSource": "MIC",
      "sampleRate": 16000,
      "channelCount": 2,
      "audioFormat": 16,
      "minBufferSize": 320,
      "bufferMultiplier": 2,
      "audioFilePath": "/data/recorded_16k_2ch_16bit.wav",
      "description": "麦克风录音配置"
    }
  ]
}
```

### 支持的常量值

**Audio Source (音频源):**
- `MIC` - 麦克风
- `VOICE_UPLINK` - 语音通话上行 (需系统权限)
- `VOICE_DOWNLINK` - 语音通话下行 (需系统权限)
- `VOICE_CALL` - 语音通话双向 (需系统权限)
- `CAMCORDER` - 摄像录音
- `VOICE_RECOGNITION` - 语音识别
- `VOICE_COMMUNICATION` - 语音通信
- `REMOTE_SUBMIX` - 远程子混音 (需系统权限)
- `UNPROCESSED` - 未处理音频
- `VOICE_PERFORMANCE` - 语音性能

**Audio Format (音频格式):**
- `16` - 16位整数
- `24` - 24位整数
- `32` - 32位整数

## 📝 智能文件命名

### 自动命名规则

当配置中的 `audioFilePath` 为空时，系统会自动生成文件名：

```
rec_YYYYMMDD_HHMMSS_mmm_[sampleRate]k_[channels]_[bitDepth]bit.wav
```

**示例文件名:**
- `rec_20240124_143052_123_16k_stereo_16bit.wav`
- `rec_20240124_143052_456_48k_mono_16bit.wav`
- `rec_20240124_143052_789_48k_stereo_16bit.wav`

## � 技术细节

### AudioRecord集成

- 使用AudioRecord API实现音频录制
- 支持多种音频格式 (16/24/32位PCM)
- 完整的错误处理机制
- 实时WAV文件写入

### 数据流架构

```
麦克风 → AudioRecord → Audio Buffer → WaveFile → WAV文件
                              ↓
                         Kotlin协程 → UI状态更新
```

### WAV文件写入

- **实时写入**: 录音过程中持续写入音频数据
- **格式支持**: 标准RIFF/WAVE格式
- **多声道支持**: 1-16声道录制
- **采样率范围**: 8kHz - 192kHz
- **位深度支持**: 8/16/24/32位

## 📚 API 参考

### AudioRecorder 类
```kotlin
class AudioRecorder {
    fun setConfig(config: AudioConfig)                      // 设置配置
    fun startRecording(): Boolean                           // 开始录音
    fun stopRecording(): Boolean                            // 停止录音
    fun isRecording(): Boolean                              // 检查录音状态
    fun setRecordingListener(listener: RecordingListener?)  // 设置监听器
}
```

## 🐛 故障排除

### 常见问题
1. **录音失败** - 确认已授予录音权限，检查设备麦克风
2. **权限问题** - `adb shell setenforce 0`
3. **配置加载失败** - 检查JSON格式是否正确

### 调试信息
```bash
adb logcat -s AudioRecorder MainActivity
```

## 📊 性能指标

- **采样率**: 8kHz - 192kHz
- **声道数**: 1-16声道
- **位深度**: 8/16/24/32位
- **缓冲区**: 可配置缓冲区大小和倍数

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**注意**: 本项目仅用于学习和测试目的，请确保在合适的设备和环境中使用，并遵守相关的录音法律法规。