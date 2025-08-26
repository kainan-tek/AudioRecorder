package com.example.audiorecorder.viewmodel

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.audiorecorder.recorder.AudioRecorder
import com.example.audiorecorder.recorder.RecorderState
import kotlinx.coroutines.launch

class RecorderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioRecorder: AudioRecorder = AudioRecorder(application)
    
    private val _recorderState = MutableLiveData<RecorderState>()
    val recorderState: LiveData<RecorderState> = _recorderState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // 用于存储observeForever的观察者，以便在ViewModel清理时移除
    private val stateObserver = Observer<RecorderState> { state ->
        _recorderState.postValue(state)
    }
    
    private val errorObserver = Observer<String> { error ->
        _errorMessage.postValue(error)
    }
    
    init {
        // 设置初始状态为IDLE
        _recorderState.postValue(RecorderState.IDLE)

        // 观察AudioRecorder的状态变化
        audioRecorder.recorderState.observeForever(stateObserver)
        audioRecorder.errorMessage.observeForever(errorObserver)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (recorderState.value == RecorderState.RECORDING) {
            return
        }
        
        viewModelScope.launch {
            try {
                // 无需设置路径，AudioRecorder会自动处理
                audioRecorder.startRecording()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        if (recorderState.value != RecorderState.RECORDING) {
            return
        }
        
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
        // 移除观察者，避免内存泄漏
        audioRecorder.recorderState.removeObserver(stateObserver)
        audioRecorder.errorMessage.removeObserver(errorObserver)
        // 释放资源
        audioRecorder.release()
    }
}
