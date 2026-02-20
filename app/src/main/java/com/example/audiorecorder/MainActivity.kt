package com.example.audiorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
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
        if (!hasRequiredPermissions()) requestRequiredPermissions()
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
            if (!hasRequiredPermissions()) {
                requestRequiredPermissions()
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
     * Handle audio recording errors with user-friendly messages
     */
    @SuppressLint("SetTextI18n")
    private fun handleError(error: String) {
        Log.e(TAG, "Audio recording error: $error")
        
        val userMessage = getUserFriendlyErrorMessage(error)
        
        AlertDialog.Builder(this)
            .setTitle("Recording Error")
            .setMessage(userMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                viewModel.clearError()
            }
            .setCancelable(true)
            .setOnCancelListener {
                viewModel.clearError()
            }
            .show()
        
        statusText.text = "Error: $userMessage"
        updateButtonStates(RecorderState.ERROR)
    }
    
    /**
     * Convert technical error message to user-friendly message
     */
    private fun getUserFriendlyErrorMessage(error: String): String {
        return when {
            error.startsWith("[FILE]", ignoreCase = true) -> 
                "Unable to create recording file. Please check storage permissions and available space."
            
            error.startsWith("[STREAM]", ignoreCase = true) -> 
                "Audio system initialization failed. Please try again."
            
            error.startsWith("[PERMISSION]", ignoreCase = true) -> 
                "Microphone access permission is required. Please grant the permission in Settings."
            
            error.startsWith("[PARAM]", ignoreCase = true) -> 
                "Invalid audio configuration. Please select a different configuration."
            
            else -> "Recording failed. Please try again."
        }
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

    /**
     * Get required permissions for recording
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> {
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            else -> {
                arrayOf(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request required permissions
     */
    private fun requestRequiredPermissions() {
        val permissions = getRequiredPermissions()
        val deniedPermissions = permissions.filter {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }

        if (deniedPermissions.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs microphone and storage access permissions to record and save audio files.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            val message = if (allGranted) {
                getString(R.string.permission_granted)
            } else {
                val deniedCount = grantResults.count { it != PackageManager.PERMISSION_GRANTED }
                "${getString(R.string.permission_required)} ($deniedCount permission(s) denied)"
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
