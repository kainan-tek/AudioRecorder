package com.example.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.recorder.RecorderState
import com.example.audiorecorder.viewmodel.RecorderViewModel
import com.example.audiorecorder.utils.audioSourceToString
import com.example.audiorecorder.utils.audioFormatToString
import com.example.audiorecorder.utils.channelCountToString
import android.widget.Button

/**
 * Concise audio recorder main interface
 * Supports loading audio configurations from external JSON files for convenient testing of different scenarios
 * 
 * Usage instructions:
 * 1. adb root && adb remount && adb shell setenforce 0
 * 2. Push configuration file to device: adb push audio_recorder_configs.json /data/
 * 3. Install and run the application
 * 4. In the app, click the "Configuration" button and select "Reload configuration file" to apply changes
 * 5. Recording files are saved to /data/recorded_audio.wav by default
 * 
 * System requirements: Android 13 (API 33+)
 * 
 * JSON configuration file format:
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
 *       "description": "Custom configuration name"
 *     }
 *   ]
 * }
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecorderViewModel
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var configButton: Button
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView

    companion object {
        private const val TAG = "MainActivity"
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
        startButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        configButton = findViewById(R.id.configButton)
        statusText = findViewById(R.id.statusTextView)
        fileInfoText = findViewById(R.id.recordingInfoTextView)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[RecorderViewModel::class.java]
        
        // Observe recording state
        viewModel.recorderState.observe(this) { state ->
            updateUI(state)
            updateRecordingInfo()
        }
        
        // Observe status messages
        viewModel.statusMessage.observe(this) { message ->
            statusText.text = message
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { error -> 
            error?.let { handleError(it) }
        }
        
        // Observe current configuration
        viewModel.currentConfig.observe(this) { config ->
            config?.let { 
                configButton.text = getString(R.string.audio_config_format, it.description)
                updateRecordingInfo(it)
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
     * Handle audio recording errors
     */
    private fun handleError(error: String) {
        Log.e(TAG, "Audio recording error: $error")
        showToast("Recording error: $error")
        
        // Reset recorder state
        resetRecorderState()
    }
    
    /**
     * Reset recorder state
     */
    private fun resetRecorderState() {
        startButton.isEnabled = true
        stopButton.isEnabled = false
        configButton.isEnabled = true
        statusText.text = getString(R.string.status_ready)
    }

    private fun showConfigSelectionDialog() {
        val configs = viewModel.getAllAudioConfigs()
        if (configs.isEmpty()) {
            showToast("No available configurations")
            return
        }
        
        val items = configs.map { it.description }.toMutableList().apply {
            add("🔄 Reload configuration file")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Select Recording Configuration")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == configs.size) {
                    reloadConfigurations()
                } else {
                    viewModel.setAudioConfig(configs[which])
                    showToast("Switched to: ${configs[which].description}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Reload configuration file
     */
    private fun reloadConfigurations() {
        try {
            viewModel.reloadConfigurations()
            showToast("Reloading configuration file...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload configurations", e)
            showToast("Configuration reload failed: ${e.message}")
        }
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
                configButton.isEnabled = false  // Disable configuration changes during recording
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
        try {
            viewModel.stopRecording()
            Log.d(TAG, "AudioRecorder resources released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecorder resources", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop recording when app goes to background
        if (viewModel.recorderState.value == RecorderState.RECORDING) {
            viewModel.stopRecording()
            Log.d(TAG, "Recording stopped due to app going to background")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecordingInfo() {
        viewModel.currentConfig.value?.let { config ->
            updateRecordingInfo(config)
        } ?: run {
            fileInfoText.text = "Record information"
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun updateRecordingInfo(config: AudioConfig) {
        val configInfo = "Current configuration: ${config.description}\n" +
                "Source: ${config.audioSource.audioSourceToString()}\n" +
                "Parameters: ${config.sampleRate}Hz | ${config.channelCount.channelCountToString()} | ${config.audioFormat.audioFormatToString()}\n" +
                "File: ${config.audioFilePath}"
        fileInfoText.text = configInfo
    }
}
