package com.example.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.audiorecorder.recorder.RecorderState
import com.example.audiorecorder.viewmodel.RecorderViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecorderViewModel
    
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[RecorderViewModel::class.java]

        setupViews()
        setupObservers()
        requestPermissions()
    }

    private fun setupViews() {
        startButton = findViewById(R.id.button1)
        stopButton = findViewById(R.id.button2)
        statusTextView = findViewById(R.id.statusTextView)

        startButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.startRecording()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            viewModel.stopRecording()
        }
    }

    private fun setupObservers() {
        // 观察录音状态变化
        viewModel.recorderState.observe(this, Observer { state ->
            updateUI(state)
        })

        // 观察错误消息
        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                // 这里可以显示错误提示
                statusTextView.text = it
            }
        })
    }

    private fun updateUI(state: RecorderState) {
        when (state) {
            RecorderState.IDLE -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                statusTextView.text = getString(R.string.ready_to_record)
            }
            RecorderState.RECORDING -> {
                startButton.isEnabled = false
                stopButton.isEnabled = true
                statusTextView.text = getString(R.string.recording)
            }
            RecorderState.PAUSED -> {
                startButton.isEnabled = true
                stopButton.isEnabled = true
                statusTextView.text = getString(R.string.paused)
            }
            RecorderState.ERROR -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                statusTextView.text = getString(R.string.error_occurred)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun requestPermissions() {
        // 检查录音权限
        val hasAudioPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasAudioPermission) {
            // 请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 所有权限都已授予，更新UI状态
            updateUI(RecorderState.IDLE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予
                updateUI(RecorderState.IDLE)
            } else {
                // 权限被拒绝
                statusTextView.text = getString(R.string.permissions_required)
                // 如果用户选择了"不再询问"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // 可以引导用户去设置中开启权限
                    statusTextView.text = getString(R.string.permissions_required_settings)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel会自动清理资源
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
