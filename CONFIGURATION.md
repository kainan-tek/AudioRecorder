# 音频录音配置系统详解

AudioRecorder 应用包含一个完整的配置系统，支持通过外部JSON文件灵活配置音频录音参数。

## 目录
- [配置文件格式](#配置文件格式)
- [配置系统架构](#配置系统架构)  
- [配置参数详解](#配置参数详解)
- [配置示例](#配置示例)
- [预设配置场景](#预设配置场景)
- [配置文件管理](#配置文件管理)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

## 配置文件格式

### JSON结构示例
```json
{
  "configs": [
    {
      "audioSource": "MIC",
      "sampleRate": 48000,
      "channelCount": 2,
      "audioFormat": 16,
      "bufferMultiplier": 4,
      "outputFilePath": "/data/recorded_audio.wav",
      "minBufferSize": 960,
      "description": "麦克风录音 (立体声)"
    }
  ]
}
```

### 使用方法
1. **首次运行**: 应用会自动从assets复制默认配置到 `/data/audio_configs.json`
2. **修改配置**: 直接编辑 `/data/audio_configs.json` 文件
3. **应用更改**: 在应用中选择"重新加载配置文件"

## 配置系统架构
### 配置文件层次
1. **默认配置**: `app/src/main/assets/audio_configs.json` (内置)
2. **外部配置**: `/data/audio_configs.json` (可修改)

### 加载机制
- 首次运行时，自动从assets复制默认配置到外部路径
- 优先加载外部配置文件
- 外部文件不存在时回退到默认配置
- 支持运行时重新加载配置

## 配置参数详解

### 音频源 (audioSource)
控制录音的音频输入源，基于Android MediaRecorder.AudioSource常量：

#### 基础音频源 (普通应用可用)
- `DEFAULT` - 默认音频源，系统自动选择最合适的音频源
- `MIC` - 麦克风音频源，标准录音应用的首选
- `CAMCORDER` - 摄像头录音，针对视频录制优化，与摄像头方向一致
- `VOICE_RECOGNITION` - 语音识别优化，针对语音识别应用调优
- `VOICE_COMMUNICATION` - 语音通话优化，适用于VoIP等通话应用
- `VOICE_PERFORMANCE` - 语音性能优化，用于实时处理和回放（如卡拉OK）
- `UNPROCESSED` - 未处理音频，提供原始音频信号（如果设备支持）

#### 系统级音频源 (需要特殊权限)
- `VOICE_UPLINK` - 语音通话上行信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `VOICE_DOWNLINK` - 语音通话下行信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `VOICE_CALL` - 语音通话双向信号 (需要CAPTURE_AUDIO_OUTPUT权限)
- `REMOTE_SUBMIX` - 远程子混音，用于捕获内部音频流的混音 (需要CAPTURE_AUDIO_OUTPUT权限)
- `ECHO_REFERENCE` - 回声参考信号，用于回声消除 (需要CAPTURE_AUDIO_OUTPUT权限)
- `RADIO_TUNER` - 广播调谐器输出 (需要CAPTURE_TUNER_AUDIO_INPUT权限)
- `HOTWORD` - 热词检测，低优先级软件热词检测 (需要CAPTURE_AUDIO_HOTWORD权限)
- `ULTRASOUND` - 超声波录音，如果设备支持 (需要ACCESS_ULTRASOUND权限)

> **重要说明**: 
> - **系统级音频源**需要系统级权限，普通第三方应用无法使用
> - **UNPROCESSED** 音频源并非所有设备都支持，使用前建议检查设备兼容性
> - **ULTRASOUND** 需要设备硬件支持超声波频率
> - 不同音频源会应用不同的音频处理算法，选择合适的音频源对录音质量很重要
> - 某些音频源可能在不同Android版本中有不同的行为表现

### 采样率 (sampleRate)
控制音频采样频率，影响音质和文件大小：

- `8000` - 电话质量
- `16000` - 语音识别标准
- `44100` - CD音质
- `48000` - 专业音频标准
- `96000` - 高保真音频

### 声道数量 (channelCount)
控制录音声道数，支持1-16声道：

- `1` - 单声道 (节省空间)
- `2` - 立体声 (标准录音)
- `4` - 四声道录音
- `6` - 5.1环绕声
- `8` - 7.1环绕声
- `12` - 7.1.4 Dolby Atmos
- `16` - 最大专业多声道配置

### 音频格式 (audioFormat)
控制音频位深度，使用数字表示：

- `8` - 8位PCM (最小文件)
- `16` - 16位PCM (标准质量)
- `24` - 24位PCM (高质量)
- `32` - 32位PCM (最高质量)

### 缓冲区配置
- `bufferMultiplier` - 缓冲区倍数 (1-8)
- `minBufferSize` - 最小缓冲区大小 (字节)

## 配置示例

### 常用场景配置

#### 标准麦克风录音
```json
{
  "audioSource": "MIC",
  "sampleRate": 48000,
  "channelCount": 2,
  "audioFormat": 16,
  "bufferMultiplier": 4,
  "outputFilePath": "/data/standard_recording.wav",
  "minBufferSize": 960,
  "description": "标准麦克风录音"
}
```

#### 语音识别录音
```json
{
  "audioSource": "VOICE_RECOGNITION",
  "sampleRate": 16000,
  "channelCount": 1,
  "audioFormat": 16,
  "bufferMultiplier": 2,
  "outputFilePath": "/data/voice_recognition.wav",
  "minBufferSize": 320,
  "description": "语音识别优化录音"
}
```

#### 高保真录音
```json
{
  "audioSource": "MIC",
  "sampleRate": 96000,
  "channelCount": 2,
  "audioFormat": 24,
  "bufferMultiplier": 8,
  "outputFilePath": "/data/hifi_recording.wav",
  "minBufferSize": 1920,
  "description": "高保真录音"
}
```

#### 多声道录音 (16声道)
```json
{
  "audioSource": "MIC",
  "sampleRate": 96000,
  "channelCount": 16,
  "audioFormat": 24,
  "bufferMultiplier": 8,
  "outputFilePath": "/data/multichannel_recording.wav",
  "minBufferSize": 15360,
  "description": "16声道专业录音"
}
```

#### 低延迟录音
```json
{
  "audioSource": "VOICE_PERFORMANCE",
  "sampleRate": 48000,
  "channelCount": 1,
  "audioFormat": 16,
  "bufferMultiplier": 1,
  "outputFilePath": "/data/low_latency.wav",
  "minBufferSize": 240,
  "description": "低延迟录音"
}
```

## 预设配置场景

应用包含19种预设配置，按音频源分类：

### 基础音频源 (普通应用可用)
- **默认音频源** - 系统自动选择最合适的音频源
- **麦克风录音** - 立体声、单声道、高保真三种配置
- **摄像头录音** - 针对视频录制优化
- **语音识别** - 16kHz单声道，针对语音识别优化
- **语音通话** - VoIP优化，包含标准和高质量版本
- **语音性能** - 实时处理优化，适用于卡拉OK等场景
- **未处理音频** - 原始信号录音，包含标准和高保真版本

### 系统级音频源 (需要特殊权限)
- **语音通话上行** - 通话发送方向音频 (8kHz)
- **语音通话下行** - 通话接收方向音频 (8kHz)
- **语音通话双向** - 完整通话音频 (8kHz)
- **远程子混音** - 内部音频流录音
- **回声参考信号** - 用于回声消除的参考信号
- **广播调谐器** - 广播电台音频输出
- **热词检测** - 低优先级热词检测音频
- **超声波录音** - 超声波频率录音 (96kHz)

每种音频源都配置了最适合的采样率、声道和缓冲区参数。详细配置参数请查看 `app/src/main/assets/audio_configs.json` 文件。

> **注意**: 系统级音频源需要相应的系统权限，普通第三方应用无法使用这些音频源。

## 配置文件管理

### 基本操作
```bash
# 查看当前配置
adb pull /data/audio_configs.json && cat audio_configs.json

# 修改配置
adb pull /data/audio_configs.json
# 编辑文件后
adb push audio_configs.json /data/

# 恢复默认配置
adb shell rm /data/audio_configs.json
```

## 配置验证

系统会自动验证配置参数：
- 检查JSON格式正确性
- 验证参数值有效性
- 提供详细的错误信息
- 无效配置时回退到默认值

## 最佳实践

### 语音录音
- 使用 `VOICE_RECOGNITION` 或 `VOICE_COMMUNICATION` 音频源
- 选择16kHz采样率和单声道配置
- 使用较小的缓冲区倍数 (2-4)

### 高质量录音
- 使用 `MIC` 音频源
- 选择48kHz或更高采样率
- 使用立体声和高位深度
- 增大缓冲区倍数 (6-8)

### 实时录音
- 使用 `VOICE_PERFORMANCE` 音频源
- 设置较小的缓冲区倍数 (1-2)
- 选择合适的采样率平衡质量和延迟

## 故障排除

### 配置加载失败
- 检查JSON格式是否正确
- 确认文件路径和权限
- 查看应用日志获取详细错误信息

### 录音问题
- 检查录音权限是否授予
- 验证输出文件路径是否可写
- 确认设备支持指定的音频参数

### 性能问题
- 调整缓冲区大小
- 选择合适的采样率
- 根据使用场景优化配置参数

### 音质问题
- 检查音频源选择是否合适
- 调整采样率和位深度
- 验证声道配置是否正确