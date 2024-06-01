package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
// import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
// import java.text.SimpleDateFormat
// import java.util.Calendar
// import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var isStart = false
    private var numOfMinBuf = 2
    private var minBufSizeInBytes = 0
    private var audioRecord: AudioRecord? = null

    companion object {
        private const val LOG_TAG = "AudioRecorder"
        // MIC VOICE_UPLINK VOICE_CALL VOICE_RECOGNITION VOICE_COMMUNICATION
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        // private const val CHANNELS = 1       // just for dump file name
        // private const val PCM_ENCODING = 16  // just for dump file name
        // private const val DUMP_FILE = "${SAMPLE_RATE}Hz_${CHANNELS}ch_${PCM_ENCODING}bit_record.pcm"
        private const val DUMP_FILE = "/data/record_48k_1ch_16bit.raw" // need to create the file manually
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
        minBufSizeInBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING)
        Log.i(LOG_TAG, "AudioRecord getMinBufferSize: $minBufSizeInBytes")

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
                    .build())
            .setBufferSizeInBytes(minBufSizeInBytes * numOfMinBuf)
            .build()

        Log.i(LOG_TAG, "set AudioRecord params: " +
                "source ${AUDIO_SOURCE}, " +
                "SampleRate ${SAMPLE_RATE}, " +
                "ChannelMask ${CHANNEL_MASK}, " +
                "Encoding $ENCODING, " +
                "BufferSizeInFrames ${audioRecord!!.bufferSizeInFrames}")

        // specify the device address with setPreferredDevice
        /*
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        for (device in devices) {
            Log.i(LOG_TAG,"device address: ${device.address}")
            if (device.address == "Built-In Mic"){
                audioRecord!!.setPreferredDevice(device)
                break
            }
        }
        */
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
                    val buffer = ByteArray(minBufSizeInBytes)
                    // val currentDate = Calendar.getInstance().time
                    // val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
                    // val formattedDate = dateFormat.format(currentDate)
                    // dump file path: /storage/emulated/0/Android/data/com.example.audiorecorder/files/Music
                    // val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${formattedDate}_${DUMP_FILE}")
                    val outputFile = File(DUMP_FILE)
                    fileOutputStream = FileOutputStream(outputFile)
                    Log.i(LOG_TAG, "record file: $outputFile")

                    audioRecord?.startRecording()

                    while (audioRecord != null) {
                        if (isStart) {
                            val bytesRead = audioRecord?.read(buffer, 0, minBufSizeInBytes)!!
                            if (bytesRead > 0) fileOutputStream.write(buffer,0,bytesRead)
                        } else {
                            stopCapture()
                        }
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
        isStart = true
        AudioRecordThread().start()
    }

    private fun stopAudioCapture() {
        Log.i(LOG_TAG,"stop AudioCapture, isStart: $isStart")
        if (isStart) {
            isStart = false
        }
    }

    private fun stopCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
