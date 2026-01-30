# AudioRecorder

一个基于Android AudioRecord API的高性能音频录制器测试程序，支持15种录音配置和实时WAV文件写入。

## 📋 项目概述

AudioRecorder是一个专为Android平台设计的音频录制测试工具，使用Android AudioRecord API。该项目展示了如何在Android应用中实现高质量的音频录制，支持多种录音使用场景和音频源类型，是音频开发者和测试工程师的理想工具。

## ✨ 主要特性

- **🎙️ 高性能录音**: 基于AudioRecord API实现音频录制
- **🔧 15种录音预设**: 涵盖麦克风、语音通话、摄像、高性能等录音场景
- **📱 现代化界面**: Material Design 3风格的直观控制界面
- **🎵 多格式支持**: 支持PCM 8位、16位、24位、32位格式
- **⚡ 实时处理**: 音频数据实时写入WAV文件，支持连续录制
- **🛠️ 动态配置**: 运行时切换录音配置，支持JSON配置文件
- **📝 智能命名**: 自动生成带时间戳的录音文件名
- **🏗️ MVVM架构**: 清晰的代码结构和模块化设计
- **🔊 多声道支持**: 支持1-16声道录音

## 🚀 快速开始

### 系统要求

- Android 12 (API 32) 或更高版本
- 支持AudioRecord的设备
- 开发环境: Android Studio

### 权限要求

- `RECORD_AUDIO`: 录音权限 (核心功能必需)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/kainan-tek/AudioRecorder.git
   cd AudioRecorder
   ```

2. **编译安装**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **可选配置文件**
   ```bash
   # 推送自定义配置文件（可选）
   adb root && adb remount && adb shell setenforce 0
   adb push audio_recorder_configs.json /data/
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
   - 点击"🔄 重新加载配置文件"刷新外部配置

### 配置切换流程

1. 点击"配置"按钮查看所有可用配置
2. 选择所需的录音场景配置
3. 配置会立即生效，显示在界面上
4. 开始录音测试音频效果

## 🎙️ 音频格式支持

### 声道配置
| 声道数  | 配置名称        | 说明            | 应用场景                    |
|------|-------------|---------------|---------------------------|
| 1    | 单声道         | Mono          | 语音录制、通话录音               |
| 2    | 立体声         | Stereo        | 标准录音、音乐录制               |
| 4    | 四声道         | Quad          | 专业录音、环境声录制              |
| 6    | 5.1声道       | 5.1 Surround  | 多声道录音、影视制作              |
| 8    | 7.1声道       | 7.1 Surround  | 高端录音设备、专业制作             |
| 1-16 | 其他配置        | 自动映射          | 根据声道数自动选择最佳配置           |

### 音频参数
- **采样率**: 8kHz - 192kHz
- **位深度**: 8/16/24/32 bit  
- **格式**: WAV (PCM)
- **最大声道**: 16声道
- **配置系统**: 支持多种音频源和缓冲区配置

## 🎙️ 15种预设配置场景

### 基础音频源 (普通应用可用)
1. **默认音频源** - 系统自动选择最合适的音频源 (44.1kHz立体声)
2. **麦克风录音** - 通用录音场景 (44.1kHz立体声)
3. **摄像录音** - 视频录制音频 (44.1kHz立体声)
4. **语音识别** - 语音识别优化 (16kHz单声道)
5. **语音通信** - VoIP通话优化 (16kHz单声道)
6. **未处理音频** - 原始信号录制 (48kHz立体声)
7. **语音性能** - 实时处理录制 (48kHz单声道)

### 系统级音频源 (需要特殊权限)
8. **语音通话上行** - 语音通信发送 (8kHz单声道，需系统权限)
9. **语音通话下行** - 语音通信接收 (8kHz单声道，需系统权限)
10. **语音通话双向** - 完整通话录制 (8kHz单声道，需系统权限)
11. **远程子混音** - 内部音频流 (44.1kHz立体声，需系统权限)
12. **回声参考信号** - 用于回声消除 (48kHz立体声，需系统权限)
13. **广播调谐器** - 广播电台音频 (44.1kHz立体声，需系统权限)
14. **热词检测** - 低优先级热词检测 (16kHz单声道，需系统权限)
15. **超声波录音** - 超声波频率录音 (96kHz单声道，需系统权限)

> **重要说明**: 系统级音频源需要相应的系统权限，普通第三方应用无法使用这些音频源。
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
      "bufferMultiplier": 2,
      "audioFilePath": "/data/recorded_16k_2ch_16bit.wav",
      "description": "麦克风录音配置"
    }
  ]
}
```

### 支持的常量值

**Audio Source (音频源):**

#### 基础音频源 (普通应用可用)
- `DEFAULT` - 默认音频源，系统自动选择
- `MIC` - 麦克风音频源，标准录音应用首选
- `CAMCORDER` - 摄像头录音，针对视频录制优化
- `VOICE_RECOGNITION` - 语音识别优化
- `VOICE_COMMUNICATION` - 语音通话优化，适用于VoIP
- `VOICE_PERFORMANCE` - 语音性能优化，用于实时处理
- `UNPROCESSED` - 未处理音频，提供原始音频信号

#### 系统级音频源 (需要特殊权限)
- `VOICE_UPLINK` - 语音通话上行信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `VOICE_DOWNLINK` - 语音通话下行信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `VOICE_CALL` - 语音通话双向信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `REMOTE_SUBMIX` - 远程子混音 (需要CAPTURE_AUDIO_OUTPUT权限)
- `ECHO_REFERENCE` - 回声参考信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `RADIO_TUNER` - 广播调谐器输出 (需要CAPTURE_TUNER_AUDIO_INPUT权限)
- `HOTWORD` - 热词检测 (需要CAPTURE_AUDIO_HOTWORD权限)
- `ULTRASOUND` - 超声波录音 (需要ACCESS_ULTRASOUND权限)

**Audio Format (音频格式):**
- `8` - 8位整数 (基础质量，节省存储空间)
- `16` - 16位整数 (标准质量，推荐使用)
- `24` - 24位整数 (高质量，专业录音)
- `32` - 32位整数 (最高质量，占用空间大)

**Sample Rate (采样率):**
- `8000` - 电话质量 (语音通话)
- `16000` - 语音识别标准
- `44100` - CD音质 (标准录音)
- `48000` - 专业音频标准 (推荐)
- `96000` - 高保真音频 (专业制作)

**Channel Count (声道数):**
- `1` - 单声道 (节省空间，语音录制)
- `2` - 立体声 (标准录音，音乐录制)
- `4-16` - 多声道录音 (专业用途，环境录制)

## 📝 智能文件命名

### 自动命名规则

当配置中的 `audioFilePath` 为空时，系统会自动生成文件名：

```
recording_[sampleRate]Hz_[channels]ch_[bitDepth]bit_[timestamp].wav
```

**示例文件名:**
- `recording_16000Hz_2ch_16bit_20240124_143052.wav`
- `recording_48000Hz_1ch_16bit_20240124_143052.wav`
- `recording_44100Hz_2ch_24bit_20240124_143052.wav`

### 文件存储位置

- **默认位置**: 应用私有目录 (`/data/data/com.example.audiorecorder/files/`)
- **自定义位置**: 通过配置文件指定完整路径
- **外部存储**: 可配置到 `/data/` 目录 (需要相应权限)

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
- **最低版本**: Android 12 (API 32)
- **目标版本**: Android 15 (API 36)
- **编译版本**: Android 15 (API 36)

### 依赖库

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

## 🔍 技术细节

### AudioRecord集成

- 使用AudioRecord API实现音频录制
- 支持多种音频格式 (8/16/24/32位PCM)
- 完整的错误处理机制
- 实时WAV文件写入
- 智能缓冲区管理

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
- **自动文件头**: 录制完成后自动更新WAV文件头

### 权限管理

- 运行时权限请求
- 权限状态检查
- 用户友好的权限说明
- 权限被拒绝时的优雅处理

## 📚 API 参考

### AudioRecorder 类
```kotlin
class AudioRecorder(context: Context) {
    fun setAudioConfig(config: AudioConfig)                      // 设置配置
    fun startRecording(): Boolean                                // 开始录音
    fun stopRecording(): Boolean                                 // 停止录音
    fun release()                                                // 释放资源
    fun setRecordingListener(listener: RecordingListener?)       // 设置监听器
}
```

### RecorderViewModel 类
```kotlin
class RecorderViewModel : ViewModel() {
    val recorderState: LiveData<RecorderState>                   // 录音状态
    val statusMessage: LiveData<String>                          // 状态消息
    val errorMessage: LiveData<String?>                          // 错误消息
    val currentConfig: LiveData<AudioConfig?>                    // 当前配置
    
    fun startRecording()                                         // 开始录音
    fun stopRecording()                                          // 停止录音
    fun setAudioConfig(config: AudioConfig)                      // 设置配置
    fun getAllAudioConfigs(): List<AudioConfig>                  // 获取所有配置
    fun reloadConfigurations()                                   // 重新加载配置
}
```

### AudioConfig 类
```kotlin
data class AudioConfig(
    val audioSource: Int,                                        // 音频源
    val sampleRate: Int,                                         // 采样率
    val channelCount: Int,                                       // 声道数
    val audioFormat: Int,                                        // 音频格式
    val bufferMultiplier: Int,                                   // 缓冲区倍数
    val audioFilePath: String,                                   // 音频文件路径
    val description: String                                      // 配置描述
) {
    val channelMask: Int                                         // 声道掩码
    
    companion object {
        fun loadConfigs(context: Context): List<AudioConfig>     // 加载配置
        fun reloadConfigs(context: Context): List<AudioConfig>   // 重新加载配置
    }
}
```

## 🐛 故障排除

### 常见问题

1. **录音失败**
   - 确认已授予录音权限
   - 检查设备麦克风是否正常
   - 验证音频参数组合是否支持

2. **权限问题**
   ```bash
   # 对于系统级音频源，需要系统权限
   adb root && adb remount && adb shell setenforce 0
   ```

3. **配置加载失败**
   - 检查JSON格式是否正确
   - 验证配置文件路径
   - 查看应用日志获取详细错误信息

4. **文件写入失败**
   - 检查输出目录是否有写入权限
   - 确认存储空间是否充足
   - 验证文件路径格式是否正确

### 调试信息
```bash
# 查看应用日志
adb logcat -s AudioRecorder MainActivity RecorderViewModel

# 检查配置文件
adb shell cat /data/audio_recorder_configs.json

# 查看录音文件
adb shell ls -la /data/recorded_*.wav
adb shell ls -la /data/data/com.example.audiorecorder/files/
```

### 性能优化建议

1. **语音录音**
   - 使用 `VOICE_RECOGNITION` 或 `VOICE_COMMUNICATION` 音频源
   - 选择16kHz采样率和单声道配置
   - 使用较小的缓冲区倍数 (2-4)

2. **高质量录音**
   - 使用 `MIC` 音频源
   - 选择48kHz或更高采样率
   - 使用立体声和高位深度
   - 增大缓冲区倍数 (6-8)

3. **实时录音**
   - 使用 `VOICE_PERFORMANCE` 音频源
   - 设置较小的缓冲区倍数 (1-2)
   - 选择合适的采样率平衡质量和延迟

### 系统级音频源说明

系统级音频源需要特殊权限，普通应用无法使用：
- `VOICE_UPLINK/DOWNLINK/CALL` - 需要 `CAPTURE_AUDIO_OUTPUT` 权限
- `REMOTE_SUBMIX` - 需要 `CAPTURE_AUDIO_OUTPUT` 权限
- `ECHO_REFERENCE` - 需要 `CAPTURE_AUDIO_OUTPUT` 权限
- `RADIO_TUNER` - 需要 `CAPTURE_TUNER_AUDIO_INPUT` 权限
- `HOTWORD` - 需要 `CAPTURE_AUDIO_HOTWORD` 权限
- `ULTRASOUND` - 需要 `ACCESS_ULTRASOUND` 权限

## 📊 性能指标

- **采样率**: 8kHz - 192kHz
- **声道数**: 1-16声道
- **位深度**: 8/16/24/32位
- **缓冲区**: 可配置缓冲区倍数 (1-8倍)
- **支持格式**: WAV (PCM)
- **最大录制时长**: 受设备存储限制
- **实时性能**: 支持连续长时间录制

## 🔗 相关项目

- **AudioPlayer**: 配套的音频播放应用，基于AudioTrack API
- **AAudioRecorder**: 基于AAudio API的高性能录音器（独立项目）
- **AAudioPlayer**: 基于AAudio API的播放器（独立项目）

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**注意**: 本项目仅用于学习和测试目的，请确保在合适的设备和环境中使用，并遵守相关的录音法律法规。