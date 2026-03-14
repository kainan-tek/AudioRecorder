package com.example.audiorecorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.recorder.AudioRecorder
import com.example.audiorecorder.recorder.RecorderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recorder ViewModel, manages UI state and recording logic
 * Supports loading audio configuration from external JSON files
 */
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application.applicationContext)

    // UI state
    private val _recorderState = MutableLiveData(RecorderState.IDLE)
    val recorderState: LiveData<RecorderState> = _recorderState

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _currentConfig = MutableLiveData<AudioConfig>()
    val currentConfig: LiveData<AudioConfig> = _currentConfig

    private val _availableConfigs = MutableLiveData<List<AudioConfig>>()

    init {
        setupRecorderListener()
        loadConfigurations()
        _statusMessage.value = "Ready to record"
    }

    /**
     * Load configuration files
     */
    private fun loadConfigurations() {
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.loadConfigs(getApplication())
            updateUI({
                _availableConfigs.value = configs
                if (configs.isNotEmpty()) {
                    val defaultConfig = configs[0]
                    audioRecorder.setAudioConfig(defaultConfig)
                    _currentConfig.value = defaultConfig
                    _statusMessage.value = "Ready to record"
                }
            })
        }
    }

    /**
     * Reload configuration files
     */
    fun reloadConfigurations() {
        if (_recorderState.value == RecorderState.RECORDING) {
            updateUI({
                _statusMessage.value = "Cannot reload configuration while recording"
                _errorMessage.value = "Please stop recording before reloading configuration"
            })
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val configs = AudioConfig.reloadConfigs(getApplication())
                updateUI({
                    if (configs.isNotEmpty()) {
                        _availableConfigs.value = configs
                        val currentConfigDescription = _currentConfig.value?.description
                        val newCurrentConfig =
                            configs.find { it.description == currentConfigDescription }
                                ?: configs[0]

                        audioRecorder.setAudioConfig(newCurrentConfig)
                        _currentConfig.value = newCurrentConfig
                        _statusMessage.value =
                            "Configuration reloaded successfully: ${configs.size} configs"
                    } else {
                        _statusMessage.value = "Configuration file is empty or format error"
                        _errorMessage.value = "No valid recording configuration found"
                    }
                })
            } catch (e: Exception) {
                updateUI({
                    _statusMessage.value = "Configuration reload failed"
                    _errorMessage.value = "Configuration reload failed: ${e.message}"
                })
            }
        }
    }

    fun startRecording() {
        if (_recorderState.value == RecorderState.RECORDING) return

        // Reset error state before starting new recording
        _errorMessage.value = null
        if (_recorderState.value == RecorderState.ERROR) {
            _recorderState.value = RecorderState.IDLE
        }

        updateUI({
            _statusMessage.value = "Preparing..."
        })

        viewModelScope.launch(Dispatchers.IO) {
            val success = audioRecorder.startRecording()
            if (!success) {
                updateUI({
                    if (_recorderState.value != RecorderState.ERROR) {
                        _recorderState.value = RecorderState.ERROR
                        _statusMessage.value = "Recording Failed"
                    }
                }, clearError = false)
            }
        }
    }

    fun stopRecording() {
        if (_recorderState.value != RecorderState.RECORDING) return

        _statusMessage.value = "Stopping..."
        audioRecorder.stopRecording()
    }

    fun setAudioConfig(config: AudioConfig) {
        audioRecorder.setAudioConfig(config)
        updateUI({
            _currentConfig.value = config
            _statusMessage.value = "Configuration updated: ${config.description}"
        })
    }

    fun getAllAudioConfigs(): List<AudioConfig> = _availableConfigs.value ?: emptyList()

    /**
     * Clear error state and reset to idle
     */
    fun clearError() {
        _errorMessage.value = null
        if (_recorderState.value == RecorderState.ERROR) {
            _recorderState.value = RecorderState.IDLE
            _statusMessage.value = "Ready to record"
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }

    fun release() {
        audioRecorder.release()
    }

    fun isRecording(): Boolean {
        return audioRecorder.isRecording()
    }

    private fun setupRecorderListener() {
        audioRecorder.setRecordingListener(object : AudioRecorder.RecordingListener {
            override fun onRecordingStarted() {
                updateUI({
                    _recorderState.value = RecorderState.RECORDING
                    _statusMessage.value = "Recording..."
                })
            }

            override fun onRecordingStopped() {
                updateUI({
                    _recorderState.value = RecorderState.IDLE
                    _statusMessage.value = "Recording Stopped"
                })
            }

            override fun onRecordingError(error: String) {
                updateUI({
                    _recorderState.value = RecorderState.ERROR
                    _statusMessage.value = "Recording Failed"
                    _errorMessage.value = error
                }, clearError = false)
            }
        })
    }

    /**
     * Execute UI updates on Main thread, clearing error message by default
     */
    private fun updateUI(block: () -> Unit, clearError: Boolean = true) {
        viewModelScope.launch(Dispatchers.Main) {
            block()
            if (clearError) {
                _errorMessage.value = null
            }
        }
    }
}
