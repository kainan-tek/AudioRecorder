package com.example.audiorecorder

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audiorecorder.recorder.RecorderState
import com.example.audiorecorder.viewmodel.RecorderViewModel
import com.google.android.material.button.MaterialButton

/**
 * 简洁的音频录音器主界面
 * 支持从外部JSON文件加载音频配置，方便测试不同场景
 * 
 * 使用说明:
 * 1. adb root && adb remount && adb shell setenforce 0
 * 2. 安装并运行应用 (首次运行会在/data/创建audio_configs.json)
 * 3. 修改 /data/audio_configs.json 文件来自定义配置
 * 4. 在应用中点击"配置"按钮，选择"重新加载配置文件"来应用更改
 * 5. 录音文件默认保存到 /data/recorded_audio.wav
 * 
 * 系统要求: Android 13 (API 33+)
 * 
 * JSON配置文件格式:
 * {
 *   "configs": [
 *     {
 *       "audioSource": "MIC",
 *       "sampleRate": 48000,
 *       "channelCount": 2,
 *       "audioFormat": 16,
 *       "bufferMultiplier": 4,
 *       "audioFilePath": "/data/recorded_audio.wav",
 *       "minBufferSize": 960,
 *       "description": "自定义配置名称"
 *     }
 *   ]
 * }
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecorderViewModel
    private lateinit var startButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var configButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initViewModel()
        setupClickListeners()
        checkPermissions()
    }

    private fun initViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        configButton = findViewById(R.id.configButton)
        statusText = findViewById(R.id.statusTextView)
        fileInfoText = findViewById(R.id.fileInfoTextView)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[RecorderViewModel::class.java]
        
        // 观察录音状态
        viewModel.recorderState.observe(this) { state ->
            updateUI(state)
        }
        
        // 观察状态消息
        viewModel.statusMessage.observe(this) { message ->
            statusText.text = message
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(this) { error -> 
            error?.let { showToast("错误: $it") }
        }
        
        // 观察当前配置
        viewModel.currentConfig.observe(this) { config ->
            config?.let { 
                configButton.text = getString(R.string.audio_config_format, it.description)
                // 更新文件信息显示，包含声道数信息
                val channelText = when(it.channelCount) {
                    1 -> getString(R.string.channel_mono)
                    2 -> getString(R.string.channel_stereo)
                    else -> "${it.channelCount}声道"
                }
                val bitDepthText = when(it.audioFormat) {
                    AudioFormat.ENCODING_PCM_8BIT -> "8"
                    AudioFormat.ENCODING_PCM_16BIT -> "16"
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24"
                    AudioFormat.ENCODING_PCM_32BIT -> "32"
                    else -> "16"
                }
                val configInfo = "当前配置: ${it.sampleRate}Hz | $channelText | ${bitDepthText}bit"
                fileInfoText.text = getString(R.string.file_info_with_config, configInfo)
            }
        }
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (hasAudioPermission()) {
                viewModel.startRecording()
            } else {
                requestAudioPermission()
            }
        }
        
        stopButton.setOnClickListener { 
            viewModel.stopRecording() 
        }
        
        configButton.setOnClickListener { 
            showConfigSelectionDialog() 
        }
    }

    /**
     * 显示配置选择对话框
     */
    private fun showConfigSelectionDialog() {
        val configs = viewModel.getAllAudioConfigs()
        if (configs.isEmpty()) {
            showToast("没有可用的配置")
            return
        }
        
        val items = configs.map { it.description }.toMutableList().apply {
            add("🔄 重新加载配置文件")
        }
        
        AlertDialog.Builder(this)
            .setTitle("选择录音配置 (${configs.size} 个)")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == configs.size) {
                    viewModel.reloadConfigurations()
                    showToast("正在重新加载配置...")
                } else {
                    viewModel.setAudioConfig(configs[which])
                    showToast("已切换到: ${configs[which].description}")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateUI(state: RecorderState) {
        when (state) {
            RecorderState.IDLE -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                configButton.isEnabled = true
            }
            RecorderState.RECORDING -> {
                startButton.isEnabled = false
                stopButton.isEnabled = true
                configButton.isEnabled = false  // 录音时禁用配置更改
            }
            RecorderState.ERROR -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                configButton.isEnabled = true
            }
        }
    }

    private fun checkPermissions() {
        if (!hasAudioPermission()) {
            requestAudioPermission()
        }
    }

    private fun hasAudioPermission() = 
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            val message = if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getString(R.string.permission_granted)
            } else {
                getString(R.string.permission_required)
            }
            showToast(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopRecording()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
