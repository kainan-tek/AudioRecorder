package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
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
        viewModel.recorderState.observe(this) { state ->
            updateUI(state)
        }

        viewModel.errorMessage.observe(this) {
            it?.let { statusTextView.text = it }
        }
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

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
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
                updateUI(RecorderState.IDLE)
            } else {
                statusTextView.text = getString(R.string.permissions_required)
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    statusTextView.text = getString(R.string.permissions_required_settings)
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
