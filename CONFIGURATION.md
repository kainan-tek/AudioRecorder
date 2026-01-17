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
      "channelConfig": "STEREO",
      "audioFormat": "PCM_16BIT",
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
控制录音的音频输入源：

- `MIC` - 麦克风 (标准录音)
- `VOICE_COMMUNICATION` - 语音通话 (通话录音)
- `VOICE_RECOGNITION` - 语音识别 (语音识别优化)
- `CAMCORDER` - 摄像头录音 (视频录制)
- `UNPROCESSED` - 未处理音频 (原始信号)
- `VOICE_PERFORMANCE` - 高性能语音 (低延迟)
- `DEFAULT` - 默认音频源

### 采样率 (sampleRate)
控制音频采样频率，影响音质和文件大小：

- `8000` - 电话质量
- `16000` - 语音识别标准
- `44100` - CD音质
- `48000` - 专业音频标准
- `96000` - 高保真音频

### 声道配置 (channelConfig)
控制录音声道数：

- `MONO` - 单声道 (节省空间)
- `STEREO` - 立体声 (标准录音)

### 音频格式 (audioFormat)
控制音频位深度：

- `PCM_8BIT` - 8位PCM (最小文件)
- `PCM_16BIT` - 16位PCM (标准质量)
- `PCM_24BIT` - 24位PCM (高质量)
- `PCM_32BIT` - 32位PCM (最高质量)

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
  "channelConfig": "STEREO",
  "audioFormat": "PCM_16BIT",
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
  "channelConfig": "MONO",
  "audioFormat": "PCM_16BIT",
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
  "channelConfig": "STEREO",
  "audioFormat": "PCM_24BIT",
  "bufferMultiplier": 8,
  "outputFilePath": "/data/hifi_recording.wav",
  "minBufferSize": 1920,
  "description": "高保真录音"
}
```

#### 低延迟录音
```json
{
  "audioSource": "VOICE_PERFORMANCE",
  "sampleRate": 48000,
  "channelConfig": "MONO",
  "audioFormat": "PCM_16BIT",
  "bufferMultiplier": 1,
  "outputFilePath": "/data/low_latency.wav",
  "minBufferSize": 240,
  "description": "低延迟录音"
}
```

## 预设配置场景

应用包含6种预设配置，覆盖以下场景：

**标准录音**: 麦克风立体声、麦克风单声道  
**语音应用**: 语音通话、语音识别  
**专业录音**: 未处理音频、高保真录音

详细配置参数请查看 `app/src/main/assets/audio_configs.json` 文件。

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