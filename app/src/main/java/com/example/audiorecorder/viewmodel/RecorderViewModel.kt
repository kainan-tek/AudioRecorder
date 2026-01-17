package com.example.audiorecorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audiorecorder.R
import com.example.audiorecorder.config.AudioConfig
import com.example.audiorecorder.recorder.AudioRecorder
import com.example.audiorecorder.recorder.RecorderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 录音器ViewModel，管理UI状态和录音逻辑
 */
class RecorderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioRecorder = AudioRecorder(application.applicationContext)
    
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
        _statusMessage.value = getString(R.string.ready_to_record)
    }

    private fun loadConfigurations() {
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.loadConfigs(getApplication())
            launch(Dispatchers.Main) {
                _availableConfigs.value = configs
                if (configs.isNotEmpty()) {
                    val defaultConfig = configs[0]
                    audioRecorder.setAudioConfig(defaultConfig)
                    _currentConfig.value = defaultConfig
                    _statusMessage.value = "配置已加载: ${configs.size} 个"
                }
            }
        }
    }

    fun reloadConfigurations() {
        if (_recorderState.value == RecorderState.RECORDING) {
            _statusMessage.value = "录音中无法重新加载配置"
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.reloadConfigs(getApplication())
            launch(Dispatchers.Main) {
                _availableConfigs.value = configs
                _statusMessage.value = "配置已重新加载: ${configs.size} 个"
            }
        }
    }

    fun startRecording() {
        if (_recorderState.value == RecorderState.RECORDING) return
        
        _statusMessage.value = getString(R.string.status_preparing)
        viewModelScope.launch(Dispatchers.IO) {
            audioRecorder.startRecording()
        }
    }

    fun stopRecording() {
        if (_recorderState.value != RecorderState.RECORDING) return
        
        _statusMessage.value = getString(R.string.status_stopping)
        audioRecorder.stopRecording()
    }
    
    fun setAudioConfig(config: AudioConfig) {
        audioRecorder.setAudioConfig(config)
        _currentConfig.value = config
        _statusMessage.value = "配置已更新: ${config.description}"
    }
    
    fun getAllAudioConfigs(): List<AudioConfig> = _availableConfigs.value ?: emptyList()

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }

    private fun setupRecorderListener() {
        audioRecorder.setRecordingListener(object : AudioRecorder.RecordingListener {
            override fun onRecordingStarted() {
                _recorderState.postValue(RecorderState.RECORDING)
                _statusMessage.postValue(getString(R.string.recording))
            }

            override fun onRecordingStopped() {
                _recorderState.postValue(RecorderState.IDLE)
                _statusMessage.postValue(getString(R.string.recording_stopped))
            }

            override fun onRecordingError(error: String) {
                _recorderState.postValue(RecorderState.ERROR)
                _statusMessage.postValue(getString(R.string.error_recording_failed))
                _errorMessage.postValue(error)
            }
        })
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}
