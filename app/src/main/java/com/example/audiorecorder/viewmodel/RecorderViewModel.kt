package com.example.audiorecorder.viewmodel

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audiorecorder.recorder.AudioRecorder
import com.example.audiorecorder.recorder.RecorderState
import kotlinx.coroutines.launch

class RecorderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioRecorder = AudioRecorder(application)
    
    private val _recorderState = MutableLiveData(RecorderState.IDLE)
    val recorderState: LiveData<RecorderState> = _recorderState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    init {
        audioRecorder.recorderState.observeForever { _recorderState.postValue(it) }
        audioRecorder.errorMessage.observeForever { _errorMessage.postValue(it) }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (recorderState.value == RecorderState.RECORDING) return
        
        viewModelScope.launch {
            try {
                audioRecorder.startRecording()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        if (recorderState.value != RecorderState.RECORDING) return
        
        viewModelScope.launch {
            try {
                audioRecorder.stopRecording()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to stop recording: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }
}
