package com.example.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audiorecorder.recorder.RecorderState
import com.example.audiorecorder.viewmodel.RecorderViewModel


class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecorderViewModel
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var configSpinner: Spinner
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView
    
    private var isSpinnerInitialized = false

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
        if (!hasAudioPermission()) requestAudioPermission()
    }

    private fun initViews() {
        startButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        configSpinner = findViewById(R.id.configSpinner)
        statusText = findViewById(R.id.statusTextView)
        fileInfoText = findViewById(R.id.recordingInfoTextView)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[RecorderViewModel::class.java]
        
        // Observe recording state
        viewModel.recorderState.observe(this) { state ->
            updateButtonStates(state)
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
                updateRecordingInfo()
                updateSpinnerSelection(it.description)
                // Initialize spinner when config is first loaded
                if (configSpinner.adapter == null) {
                    setupConfigSpinner()
                }
            }
        }
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (!hasAudioPermission()) {
                requestAudioPermission()
                return@setOnClickListener
            }
            viewModel.startRecording()
        }
        
        stopButton.setOnClickListener {
            viewModel.stopRecording()
        }
    }
    
    /**
     * Setup configuration spinner
     */
    private fun setupConfigSpinner() {
        val configs = viewModel.getAllAudioConfigs()
        Log.d(TAG, "Setting up config spinner with ${configs.size} configurations")
        
        if (configs.isEmpty()) {
            Log.w(TAG, "No configurations available for spinner")
            return
        }
        
        val configNames = configs.map { it.description }
        Log.d(TAG, "Config names: $configNames")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        configSpinner.adapter = adapter
        
        // Set initial selection
        val currentConfig = viewModel.currentConfig.value
        currentConfig?.let {
            val index = configs.indexOfFirst { config -> config.description == it.description }
            if (index >= 0) {
                configSpinner.setSelection(index)
                Log.d(TAG, "Set initial spinner selection to index $index: ${it.description}")
            }
        }
        
        configSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    Log.d(TAG, "Spinner initialized, skipping first selection")
                    return
                }
                
                val selectedConfig = configs[position]
                Log.d(TAG, "Config selected: ${selectedConfig.description}")
                viewModel.setAudioConfig(selectedConfig)
                showToast("Switched to: ${selectedConfig.description}")
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Nothing selected in spinner")
            }
        }
        
        // Add long press listener to reload configurations
        configSpinner.setOnLongClickListener {
            Log.d(TAG, "Long press detected on spinner")
            reloadConfigurations()
            true
        }
    }
    
    /**
     * Update spinner selection based on config description
     */
    private fun updateSpinnerSelection(description: String) {
        val configs = viewModel.getAllAudioConfigs()
        val index = configs.indexOfFirst { it.description == description }
        if (index >= 0 && index != configSpinner.selectedItemPosition) {
            isSpinnerInitialized = false
            configSpinner.setSelection(index)
        }
    }

    /**
     * Update button states based on recording state
     */
    private fun updateButtonStates(state: RecorderState) {
        when (state) {
            RecorderState.IDLE -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                configSpinner.isEnabled = true
            }
            RecorderState.RECORDING -> {
                startButton.isEnabled = false
                stopButton.isEnabled = true
                configSpinner.isEnabled = false  // Disable configuration changes during recording
            }
            RecorderState.ERROR -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
                configSpinner.isEnabled = true
            }
        }
    }

    /**
     * Handle audio recording errors
     */
    @SuppressLint("SetTextI18n")
    private fun handleError(error: String) {
        Log.e(TAG, "Audio recording error: $error")
        showToast("Recording error: $error")
        statusText.text = "Error: $error"
        
        // Reset recorder state
        updateButtonStates(RecorderState.ERROR)
    }
    
    /**
     * Reload configuration file
     */
    private fun reloadConfigurations() {
        try {
            viewModel.reloadConfigurations()
            showToast("Configuration reloaded successfully")
            // Refresh spinner after reload
            isSpinnerInitialized = false
            setupConfigSpinner()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload configurations", e)
            showToast("Configuration reload failed: ${e.message}")
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // Show explanation dialog
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs microphone access permission to record audio.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
            val configInfo = "Current Config: ${config.description}\n" +
                    "Source: ${config.audioSource}\n" +
                    "Parameters: ${config.sampleRate}Hz | ${config.channelCount}ch | ${config.audioFormat}bit\n" +
                    "File: ${config.audioFilePath}"
            fileInfoText.text = configInfo
        } ?: run {
            fileInfoText.text = "Recording Info"
        }
    }
}
