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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audiorecorder.recorder.RecorderState
import com.example.audiorecorder.viewmodel.RecorderViewModel


/**
 * Audio Recorder Main Activity
 *
 * Usage Instructions:
 * 1. Grant recording permissions
 * 2. Select recording configuration from dropdown
 * 3. Start recording
 *
 * System Requirements: Android 12L (API 32+)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: RecorderViewModel
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var configSpinner: Spinner
    private lateinit var statusText: TextView
    private lateinit var recordingInfoText: TextView

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
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        configSpinner = findViewById(R.id.configSpinner)
        statusText = findViewById(R.id.statusTextView)
        recordingInfoText = findViewById(R.id.recordingInfoTextView)
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
                    Log.i(
                        TAG,
                        "Loaded ${viewModel.getAllAudioConfigs().size} recording configurations"
                    )
                }
            }
        }
    }

    private fun setupClickListeners() {
        recordButton.setOnClickListener {
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

        if (configs.isEmpty()) {
            Log.w(TAG, "No configurations available")
            return
        }

        val configNames = configs.map { it.description }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        configSpinner.adapter = adapter

        // Set initial selection
        val currentConfig = viewModel.currentConfig.value
        currentConfig?.let {
            val index = configs.indexOfFirst { config -> config.description == it.description }
            if (index >= 0) {
                configSpinner.setSelection(index)
            }
        }

        configSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val selectedConfig = configs[position]
                viewModel.setAudioConfig(selectedConfig)
                showToast("Switched to: ${selectedConfig.description}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Add long press listener to reload configurations
        configSpinner.setOnLongClickListener {
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
                recordButton.isEnabled = true
                stopButton.isEnabled = false
                configSpinner.isEnabled = true
            }

            RecorderState.RECORDING -> {
                recordButton.isEnabled = false
                stopButton.isEnabled = true
                configSpinner.isEnabled = false  // Disable configuration changes during recording
            }

            RecorderState.ERROR -> {
                recordButton.isEnabled = true
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

        AlertDialog.Builder(this).setTitle("Recording Error").setMessage(userMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                viewModel.clearError()
            }.setCancelable(true).setOnCancelListener {
                viewModel.clearError()
            }.show()

        statusText.text = "Error: $userMessage"
        updateButtonStates(RecorderState.ERROR)
    }

    /**
     * Convert technical error message to user-friendly message
     */
    private fun getUserFriendlyErrorMessage(error: String): String {
        return when {
            error.startsWith(
                "[FILE]", ignoreCase = true
            ) -> "Unable to create recording file. Please check storage permissions and available space."

            error.startsWith(
                "[STREAM]", ignoreCase = true
            ) -> "Audio system initialization failed. Please try again."

            error.startsWith(
                "[PERMISSION]", ignoreCase = true
            ) -> "Microphone access permission is required. Please grant the permission in Settings."

            error.startsWith(
                "[PARAM]", ignoreCase = true
            ) -> "Invalid audio configuration. Please select a different configuration."

            error.contains(
                "Already recording", ignoreCase = true
            ) -> "Recording is already in progress."

            error.contains(
                "Not currently recording", ignoreCase = true
            ) -> "No recording is in progress."

            else -> "Recording failed. Please try again."
        }
    }

    /**
     * Reload configuration file
     */
    @SuppressLint("SetTextI18n")
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
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE
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
    private fun hasAudioPermission(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request required permissions
     */
    private fun requestAudioPermission() {
        requestPermissions(getRequiredPermissions(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            val message = if (allGranted) {
                "Permission granted"
            } else {
                val deniedCount = grantResults.count { it != PackageManager.PERMISSION_GRANTED }
                "Recording permission required ($deniedCount permission(s) denied)"
            }
            showToast(message)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecordingInfo() {
        viewModel.currentConfig.value?.let { config ->
            val filePathDisplay = config.audioFilePath.ifBlank {
                "<App default path (auto-generated at recording start)>"
            }
            val configInfo =
                "Current Config: ${config.description}\n" + "Source: ${config.audioSource}\n" + "Parameters: ${config.sampleRate}Hz | ${config.channelCount}ch | ${config.audioFormat}bit\n" + "File: $filePathDisplay"
            recordingInfoText.text = configInfo
        } ?: run {
            recordingInfoText.text = "Recording Info"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            viewModel.release()
            Log.d(TAG, "AudioRecorder resources released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecorder resources", e)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop recording when app goes to background
        if (viewModel.isRecording()) {
            viewModel.stopRecording()
            Log.d(TAG, "Recording stopped due to app going to background")
        }
    }
}
