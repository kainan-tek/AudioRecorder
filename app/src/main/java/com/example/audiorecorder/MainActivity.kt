package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var isStart = false
    private var bufferSizeInBytes = 0
    private var audioRecord: AudioRecord? = null

    companion object {
        private const val LOG_TAG = "AudioRecorder"
        // MIC VOICE_UPLINK VOICE_CALL VOICE_RECOGNITION VOICE_COMMUNICATION
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNELS = 1       // just for dump file name
        private const val PCM_ENCODING = 16  // just for dump file name
        // dump file path: /storage/emulated/0/Android/data/com.example.audiorecorder/files/Music
        private const val DUMP_FILE = "${SAMPLE_RATE}Hz_${CHANNELS}ch_${PCM_ENCODING}bit_record.pcm"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        button1.setOnClickListener {
            startAudioCapture()
        }
        button2.setOnClickListener {
            stopAudioCapture()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            0
        )
    }

    private fun initAudioCapture() {
        Log.i(LOG_TAG, "initAudioCapture")
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING
        )
        Log.i(LOG_TAG, "AudioRecord getMinBufferSize: $bufferSizeInBytes")

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord.Builder()
            .setAudioSource(AUDIO_SOURCE)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_MASK)
                    .setEncoding(ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeInBytes*2)
            .build()
        Log.i(
            LOG_TAG, "set AudioRecord params: " +
                    "source ${AUDIO_SOURCE}, " +
                    "SampleRate ${SAMPLE_RATE}, " +
                    "ChannelMask ${CHANNEL_MASK}, " +
                    "Encoding $ENCODING"
        )
    }

    private fun startAudioCapture() {
        Log.i(LOG_TAG,"start AudioCapture, isStart: $isStart")
        if (isStart){
            Log.i(LOG_TAG,"in recording status, needn't start again")
            return
        }
        class AudioRecordThread: Thread() {
            override fun run() {
                super.run()

                var fileOutputStream: FileOutputStream? = null
                try {
                    val buffer = ByteArray(bufferSizeInBytes*2)
                    val currentDate = Calendar.getInstance().time
                    val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                    val formattedDate = dateFormat.format(currentDate)
                    val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${formattedDate}_${DUMP_FILE}")
                    fileOutputStream = FileOutputStream(outputFile)
                    Log.i(LOG_TAG, "Created file: $outputFile")

                    audioRecord?.startRecording()

                    while (audioRecord!=null) {
                        val bytesRead = audioRecord?.read(buffer, 0, bufferSizeInBytes*2)!!
                        if (bytesRead > 0) fileOutputStream.write(buffer,0,bytesRead)
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Exception: ")
                    e.printStackTrace()
                } finally {
                    try {
                        fileOutputStream?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        initAudioCapture()
        AudioRecordThread().start()
        isStart = true
    }

    private fun stopAudioCapture() {
        Log.i(LOG_TAG,"stop AudioCapture, isStart: $isStart")
        if (isStart) {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            isStart = false
        }
    }
}
