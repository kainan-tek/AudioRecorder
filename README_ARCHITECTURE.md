# AudioRecorder 架构说明

## 项目重构后的架构

本项目已按照MVVM架构模式进行了重构，实现了UI和业务逻辑的完全分离。

### 架构组件

#### 1. MainActivity (UI层)
- **职责**: 仅负责界面交互和显示
- **功能**:
  - 显示录音状态
  - 显示录音时长
  - 处理用户点击事件
  - 请求权限
- **特点**: 不包含任何录音逻辑，所有业务逻辑都委托给ViewModel

#### 2. RecorderViewModel (ViewModel层)
- **职责**: 管理UI状态和业务逻辑协调
- **功能**:
  - 使用LiveData管理录音状态
  - 提供录音控制接口
  - 管理录音文件列表
  - 处理错误信息
- **特点**: 不直接操作UI，通过LiveData与UI通信

#### 3. AudioRecorder (业务逻辑层)
- **职责**: 处理所有录音相关的操作
- **功能**:
  - 初始化AudioRecord
  - 开始/停止录音
  - 管理录音线程
  - 计算录音时长和振幅
- **特点**: 独立于Android框架，可单元测试

#### 4. WaveFile (数据层)
- **职责**: 处理WAV文件的所有操作
- **功能**:
  - 创建WAV文件
  - 写入音频数据
  - 更新WAV文件头
  - 提供文件信息查询
- **特点**: 封装了所有文件I/O操作

#### 5. RecorderState (状态管理)
- **职责**: 定义录音状态枚举
- **状态**:
  - IDLE: 空闲状态
  - RECORDING: 录音中
  - PAUSED: 暂停状态
  - ERROR: 错误状态

### 数据流

```
用户操作 → MainActivity → RecorderViewModel → AudioRecorder → WaveFile
                    ↓
                LiveData更新 ← AudioRecorder ← WaveFile
```

### 优势

1. **分离关注点**: 每个类都有明确的职责
2. **可测试性**: 业务逻辑可以独立于UI进行单元测试
3. **可维护性**: 修改UI不会影响业务逻辑，反之亦然
4. **可扩展性**: 容易添加新功能，如暂停/恢复录音
5. **生命周期感知**: ViewModel自动处理配置变化

### 使用示例

```kotlin
// 开始录音
viewModel.startRecording()

// 停止录音
viewModel.stopRecording()

// 观察状态
viewModel.recorderState.observe(this) { state ->
    // 更新UI
}
```

### 文件结构

```
com.example.audiorecorder/
├── MainActivity.kt          # UI层
├── RecorderViewModel.kt     # ViewModel层
├── AudioRecorder.kt         # 业务逻辑层
├── WaveFile.kt             # 数据层
└── RecorderState.kt        # 状态定义
```