# AudioRecorder

中文 | [English](README_EN.md)

基于 Android AudioRecord API 的音频录制器，支持多种录音配置和 WAV 文件输出。

## 目录

- [项目简介](#项目简介)
- [快速开始](#快速开始)
- [安装部署](#安装部署)
- [配置说明](#配置说明)
- [API 参考](#api-参考)
- [故障排除](#故障排除)
- [许可证](#许可证)

## 项目简介

AudioRecorder 是一个 Android 音频录制器，基于 Android AudioRecord API 开发，支持多种录音场景和 WAV
文件输出。

### 核心特性

- **15 种录音场景**: 默认、麦克风、语音通话、语音识别、摄像录音、立体声录音、回声参考、语音性能、远程混音、未处理、无线电调谐器、热词检测、超声波等
- **完整音频支持**: 1-16 声道，8kHz-192kHz 采样率，8/16/24/32 位 PCM
- **WAV 文件输出**: 自动生成 WAV 文件头，支持智能文件命名
- **灵活配置**: JSON 配置文件，支持外部热更新

### 录音场景

| 场景    | 音源                  | 采样率   | 声道  | 典型用途    |
|-------|---------------------|-------|-----|---------|
| 默认    | DEFAULT             | 48kHz | 立体声 | 默认录音    |
| 麦克风   | MIC                 | 48kHz | 立体声 | 标准录音    |
| 语音上行  | VOICE_UPLINK        | 16kHz | 单声道 | 语音通话上行  |
| 语音下行  | VOICE_DOWNLINK      | 16kHz | 单声道 | 语音通话下行  |
| 语音通话  | VOICE_CALL          | 16kHz | 单声道 | 双向语音通话  |
| 摄像录音  | CAMCORDER           | 48kHz | 立体声 | 视频录制    |
| 语音识别  | VOICE_RECOGNITION   | 16kHz | 单声道 | ASR 应用  |
| 语音通信  | VOICE_COMMUNICATION | 16kHz | 单声道 | VoIP 应用 |
| 远程混音  | REMOTE_SUBMIX       | 48kHz | 立体声 | 远程混音    |
| 未处理   | UNPROCESSED         | 48kHz | 立体声 | 无处理录音   |
| 语音性能  | VOICE_PERFORMANCE   | 48kHz | 单声道 | 专业录制    |
| 回声参考  | ECHO_REFERENCE      | 48kHz | 立体声 | AEC 参考  |
| 无线电调谐 | RADIO_TUNER         | 48kHz | 立体声 | 无线电录制   |
| 热词检测  | HOTWORD             | 16kHz | 单声道 | 低功耗热词检测 |
| 超声波   | ULTRASOUND          | 48kHz | 单声道 | 超声波录制   |

## 快速开始

### 基本使用

1. **选择配置** - 通过下拉菜单选择录音场景
2. **开始录音** - 点击绿色录音按钮
3. **停止录音** - 点击红色停止按钮
4. **重载配置** - 长按下拉菜单重新加载外部配置

### 常用操作

```bash
# 查看录音日志
adb logcat -s AudioRecorder MainActivity AudioConfig

# 检查配置文件
adb shell cat /data/audio_recorder_configs.json

# 查看录音文件
adb shell ls -la /data/recorded_*.wav
```

## 安装部署

### 环境要求

- **Android 版本**: Android 12L (API 32) 或更高
- **开发环境**: Android Studio
- **构建系统**: Gradle

### 编译安装

```bash
git clone https://github.com/kainan-tek/AudioRecorder.git
cd AudioRecorder
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 权限配置

| 权限                       | 用途     | 版本要求        |
|--------------------------|--------|-------------|
| `RECORD_AUDIO`           | 录音权限   | 全部          |
| `READ_EXTERNAL_STORAGE`  | 读取配置文件 | Android 12- |
| `WRITE_EXTERNAL_STORAGE` | 保存录音文件 | Android 9-  |

```bash
# 手动授予录音权限
adb shell pm grant com.example.audiorecorder android.permission.RECORD_AUDIO
```

## 配置说明

### 配置文件位置

- **外部配置**: `/data/audio_recorder_configs.json`（优先加载）
- **内置配置**: `app/src/main/assets/audio_recorder_configs.json`

### 配置文件格式

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

### 配置参数

#### Audio Source（音频源）

| 值                     | 说明     | 典型用途    |
|-----------------------|--------|---------|
| `DEFAULT`             | 默认     | 默认录音    |
| `MIC`                 | 麦克风    | 标准录音    |
| `VOICE_UPLINK`        | 语音上行   | 通话录音    |
| `VOICE_DOWNLINK`      | 语音下行   | 通话录音    |
| `VOICE_CALL`          | 通话双向   | 双向通话录音  |
| `CAMCORDER`           | 摄像录音   | 视频录制    |
| `VOICE_RECOGNITION`   | 语音识别   | ASR 应用  |
| `VOICE_COMMUNICATION` | 语音通话   | VoIP 应用 |
| `REMOTE_SUBMIX`       | 远程混音   | 远程混音    |
| `UNPROCESSED`         | 未处理    | 无处理录音   |
| `VOICE_PERFORMANCE`   | 语音性能   | 专业录制    |
| `ECHO_REFERENCE`      | 回声参考   | AEC 参考  |
| `RADIO_TUNER`         | 无线电调谐器 | 无线电录制   |
| `HOTWORD`             | 热词检测   | 低功耗热词检测 |
| `ULTRASOUND`          | 超声波    | 超声波录制   |

#### 采样率

| 值     | 说明      | 典型用途  |
|-------|---------|-------|
| 8000  | 8kHz    | 电话质量  |
| 16000 | 16kHz   | 语音识别  |
| 44100 | 44.1kHz | CD 质量 |
| 48000 | 48kHz   | 专业录音  |

#### 位深度

| 值  | 说明           |
|----|--------------|
| 8  | 8 位 PCM      |
| 16 | 16 位 PCM（推荐） |
| 24 | 24 位 PCM     |
| 32 | 32 位 PCM     |

### 智能文件命名

当 `audioFilePath` 为空时，自动生成带时间戳的文件名：

```
rec_YYYYMMDD_HHMMSS_mmm_[sampleRate]k_[channels]ch_[bitDepth]bit.wav
```

**示例**: `rec_20240124_143052_123_48k_1ch_16bit.wav`

## API 参考

### AudioRecorder 类

```kotlin
class AudioRecorder(private val context: Context) {
    fun setAudioConfig(config: AudioConfig)      // 设置音频配置
    fun startRecording(): Boolean                // 开始录音
    fun stopRecording()                          // 停止录音（幂等）
    fun isRecording(): Boolean                   // 检查录音状态
    fun release()                                // 释放资源
    fun setRecordingListener(listener: RecordingListener?)  // 设置监听器
}
```

### RecordingListener 接口

```kotlin
interface RecordingListener {
    fun onRecordingStarted()                     // 录音开始回调
    fun onRecordingStopped()                     // 录音停止回调
    fun onRecordingError(error: String)          // 录音错误回调
}
```

### 错误前缀

| 前缀             | 说明     |
|----------------|--------|
| `[FILE]`       | 文件操作错误 |
| `[STREAM]`     | 音频流错误  |
| `[PERMISSION]` | 权限错误   |
| `[PARAM]`      | 参数验证错误 |

## 故障排除

### 常见问题

#### 1. 录音失败

```bash
# 检查录音权限
adb shell dumpsys package com.example.audiorecorder | grep RECORD_AUDIO

# 查看详细日志
adb logcat -s AudioRecorder
```

#### 2. 权限问题

```bash
adb shell pm grant com.example.audiorecorder android.permission.RECORD_AUDIO
adb shell setenforce 0
```

#### 3. 文件保存失败

```bash
adb shell df /data          # 检查磁盘空间
adb shell ls -la /data/     # 检查文件权限
```

### 调试命令

```bash
adb logcat -s AudioRecorder MainActivity AudioConfig
adb logcat -s AudioRecord AudioFlinger
```

## 相关项目

- [AAudioPlayer](https://github.com/kainan-tek/AAudioPlayer) - 基于 AAudio API 的高性能播放器
- [AAudioRecorder](https://github.com/kainan-tek/AAudioRecorder) - 基于 AAudio API 的高性能录音器
- [AudioPlayer](https://github.com/kainan-tek/AudioPlayer) - 基于 AudioTrack API 的音频播放器
- [audio_test_client](https://github.com/kainan-tek/audio_test_client) - Android 系统级音频测试工具

## 许可证

本项目采用 MIT License 许可证。详细信息请参阅 [LICENSE](LICENSE) 文件。

---

**注意**: 本项目仅供学习和测试使用，请遵守相关录音法律法规。
