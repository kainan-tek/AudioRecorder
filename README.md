# AudioRecorder

专业的Android音频录音测试应用，支持多种录音配置和参数调节，适用于音频开发和测试场景。

## 📚 文档导航

- **[README.md](README.md)** - 项目概述、快速开始、基本使用
- **[CONFIGURATION.md](CONFIGURATION.md)** - 详细的配置系统说明、参数详解、最佳实践

## 🎯 功能特性

- **多种音频源支持** - 麦克风、语音通话、语音识别、未处理音频等
- **灵活配置管理** - JSON配置文件，支持实时加载和切换
- **高质量录音** - 支持8kHz-192kHz采样率，8/16/24/32bit位深度
- **WAV格式输出** - 标准WAV文件格式，兼容性好
- **Material Design** - 现代化UI设计，简洁易用
- **MVVM架构** - 清晰的代码结构，易于维护

## 📱 界面预览

简洁的Material Design界面，包含：
- 录音/停止按钮
- 实时状态显示
- 配置选择和管理
- 详细的录音参数信息

## 🚀 快速开始

### 安装要求
- Android 13 (API 33) 或更高版本
- 录音权限 (RECORD_AUDIO)

### 基本使用
1. 安装并启动应用
2. 授予录音权限
3. 选择合适的录音配置
4. 点击"录音"开始录制
5. 点击"停止"结束录制

## ⚙️ 配置管理

### 预设配置

应用内置6种常用录音配置：

| 配置名称 | 音频源 | 采样率 | 声道 | 位深度 | 用途 |
|---------|--------|--------|------|--------|------|
| 麦克风录音 (立体声) | MIC | 48kHz | 立体声 | 16bit | 通用录音 |
| 麦克风录音 (单声道) | MIC | 48kHz | 单声道 | 16bit | 节省空间 |
| 语音通话录音 | VOICE_COMMUNICATION | 16kHz | 单声道 | 16bit | 通话录音 |
| 语音识别录音 | VOICE_RECOGNITION | 16kHz | 单声道 | 16bit | 语音识别 |
| 未处理音频 | UNPROCESSED | 48kHz | 立体声 | 16bit | 原始信号 |
| 高保真录音 | MIC | 96kHz | 立体声 | 24bit | 高质量录音 |

### 自定义配置

应用支持通过JSON配置文件自定义录音参数。配置文件会在首次运行时自动创建到 `/data/audio_configs.json`。

**详细的配置说明请参考：[CONFIGURATION.md](CONFIGURATION.md)**

配置文档包含：
- 📋 完整的JSON配置格式
- 🔧 所有参数的详细说明
- 💡 常用场景的配置示例
- 🛠️ 配置文件管理方法
- 📖 最佳实践指南
- 🔍 故障排除方案

### 快速配置示例

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
      "description": "自定义配置名称"
    }
  ]
}
```

> 💡 **提示**: 修改配置文件后，在应用中选择"🔄 重新加载配置文件"来应用更改。

## 🛠️ 开发者指南

### 项目结构
```
app/src/main/
├── java/com/example/audiorecorder/
│   ├── MainActivity.kt              # 主界面
│   ├── config/
│   │   └── AudioConfig.kt          # 配置管理
│   ├── recorder/
│   │   ├── AudioRecorder.kt        # 录音核心
│   │   └── RecorderState.kt        # 状态定义
│   ├── model/
│   │   └── WaveFile.kt            # WAV文件处理
│   └── viewmodel/
│       └── RecorderViewModel.kt    # 视图模型
├── res/
│   ├── layout/
│   │   └── activity_main.xml       # 主界面布局
│   ├── values/
│   │   ├── colors.xml             # 颜色定义
│   │   ├── strings.xml            # 字符串资源
│   │   └── themes.xml             # 主题样式
│   └── assets/
│       └── audio_configs.json      # 默认配置
└── AndroidManifest.xml             # 应用清单
```

### 技术栈
- **语言**: Kotlin
- **架构**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3
- **音频**: Android AudioRecord API
- **异步**: Kotlin Coroutines
- **数据**: JSON配置文件

### 核心类说明

**AudioRecorder** - 录音核心类
- 管理AudioRecord实例
- 处理录音数据流
- 生成WAV文件

**AudioConfig** - 配置管理类
- 加载和解析JSON配置
- 支持内置和外部配置文件
- 配置验证和转换

**RecorderViewModel** - 视图模型
- 管理录音状态
- 处理UI交互
- 配置切换和重载

## 📋 使用说明

### ADB命令行操作
```bash
# 获取root权限并设置SELinux为宽松模式
adb root
adb remount
adb shell setenforce 0

# 安装应用
adb install AudioRecorder.apk

# 查看录音文件
adb shell ls -la /data/*.wav

# 下载录音文件到电脑
adb pull /data/recorded_audio.wav ./
```

### 配置文件管理
```bash
# 查看当前配置
adb shell cat /data/audio_configs.json

# 上传自定义配置
adb push custom_config.json /data/audio_configs.json

# 重启应用以加载新配置
```

## 🔧 故障排除

### 常见问题

**录音权限被拒绝**
- 在系统设置中手动授予录音权限
- 确保应用有存储权限 (Android 10+)

**配置相关问题**
- 配置文件加载失败
- 录音参数不生效
- 音质或性能问题

> 📖 **详细的配置故障排除请参考：[CONFIGURATION.md - 故障排除](CONFIGURATION.md#故障排除)**

**录音质量问题**
- 调整采样率和位深度
- 检查缓冲区设置
- 尝试不同的音频源

**文件保存失败**
- 确保输出路径有写入权限
- 检查存储空间是否充足
- 验证文件路径格式

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至项目维护者

---

**注意**: 本应用仅用于音频开发和测试目的，请遵守当地法律法规，不要用于非法录音活动。