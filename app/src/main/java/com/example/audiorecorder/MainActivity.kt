package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
// import java.text.SimpleDateFormat
// import java.util.Calendar
// import java.util.Locale

class MainActivity : AppCompatActivity() {
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

    /*
    AudioFormat.CHANNEL_IN_MONO
    AudioFormat.CHANNEL_IN_STEREO
    6291468 // CHANNEL_IN_2POINT0POINT2
    1507340 // CHANNEL_IN_5POINT1
    4092    // AUDIO_CHANNEL_IN_10
    16380   // AUDIO_CHANNEL_IN_12
    65532   // AUDIO_CHANNEL_IN_14
    262140  // AUDIO_CHANNEL_IN_16
    */
    companion object {
        private const val LOG_TAG = "AudioRecorder"
        private var audioFile = "/data/record_48k_1ch_16bit.wav"
        // MIC VOICE_UPLINK VOICE_CALL VOICE_RECOGNITION VOICE_COMMUNICATION
        private var source = MediaRecorder.AudioSource.MIC
        private var sampleRate = 48000
        private var channelMask = AudioFormat.CHANNEL_IN_MONO
        private var format = AudioFormat.ENCODING_PCM_16BIT

        private var isStart = false
        private var numOfMinBuf = 2
        private var minBufSizeInBytes = 0
        private var audioRecord: AudioRecord? = null
        private var fileOutputStream: FileOutputStream? = null
        private var isAudioFileCreated = false
    }

    private fun initAudioCapture(): Boolean {
        Log.i(LOG_TAG, "initAudioCapture")
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return false
        }

        minBufSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelMask, format)
        Log.i(LOG_TAG, "AudioRecord getMinBufferSize: $minBufSizeInBytes")

        audioRecord = AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .setEncoding(format)
                    .build())
            .setBufferSizeInBytes(minBufSizeInBytes * numOfMinBuf)
            .build()

        Log.i(LOG_TAG, "set AudioRecord params: " +
                "source ${source}, " +
                "SampleRate ${sampleRate}, " +
                "ChannelMask ${channelMask}, " +
                "Encoding $format, " +
                "BufferSizeInFrames ${audioRecord!!.bufferSizeInFrames}")

        // var channelCount = channelCountFromInChannelMask(channelMask)
        // var bytesPerSample = AudioFormat.getBytesPerSample(format)
        val channelCount = when (channelMask) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            6291468 -> 4  // CHANNEL_IN_2POINT0POINT2
            1507340 -> 6  // CHANNEL_IN_5POINT1
            4092 -> 10    // AUDIO_CHANNEL_IN_10
            16380 -> 12   // AUDIO_CHANNEL_IN_12
            65532 -> 14   // AUDIO_CHANNEL_IN_14
            262140 -> 16  // AUDIO_CHANNEL_IN_16
            else -> 1
        }
        val bytesPerSample = when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> 2
        }

        // val currentDate = Calendar.getInstance().time
        // val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        // val formattedDate = dateFormat.format(currentDate)
        // audioFile = "/data/record_${sampleRate/1000}k_${channelCount}ch_${bytesPerSample*8}bit_${formattedDate}.wav"
        audioFile = "/data/record_${sampleRate/1000}k_${channelCount}ch_${bytesPerSample*8}bit.wav"
        val outputFile = File(audioFile)
        try {
            fileOutputStream = FileOutputStream(outputFile)
            writeWavHeader(fileOutputStream, sampleRate, channelCount, bytesPerSample*8)
            isAudioFileCreated = true
            Log.i(LOG_TAG, "record audio file: $outputFile")
        } catch (_: SecurityException) {
            isAudioFileCreated = false
            Log.e(LOG_TAG, "no permission to access the audio file")
        } catch (_: FileNotFoundException) {
            isAudioFileCreated = false
            Log.e(LOG_TAG, "audio file can't be created or opened")
        }

        /*
        // specify the device address with setPreferredDevice
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
        return true
    }

    private fun startAudioCapture() {
        if (isStart) {
            Log.i(LOG_TAG,"in recording status, needn't start again")
            return
        }
        Log.i(LOG_TAG,"start AudioCapture")
        isStart = true
        if (!initAudioCapture()) {
            isStart = false
            Log.e(LOG_TAG,"init fail, return")
            return
        }
        startCapture()
    }

    private fun stopAudioCapture() {
        if (!isStart) {
            Log.i(LOG_TAG,"in stop status, needn't stop again")
            return
        }
        Log.i(LOG_TAG,"stop AudioCapture")
        isStart = false
    }

    private fun startCapture() {
        class AudioRecordThread: Thread() {
            override fun run() {
                super.run()

                val buffer = ByteArray(minBufSizeInBytes)
                if (audioRecord!!.state != AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord!!.startRecording()
                    sleep(5)
                }
                var totalBytesRead = 0
                while (audioRecord != null) {
                    if (isStart) {
                        val bytesRead = audioRecord?.read(buffer, 0, minBufSizeInBytes)!!
                        if ((bytesRead > 0) && (isAudioFileCreated == true)) {
                            fileOutputStream?.write(buffer,0,bytesRead)
                            totalBytesRead += bytesRead
                        }
                    } else {
                        stopCapture()
                        if (isAudioFileCreated == true) {
                            fileOutputStream?.let { updateWavHeader(it,totalBytesRead + 44) }
                        }
                    }
                }
            }
        }
        AudioRecordThread().start()
    }

    private fun stopCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }


    /*************** wav header function **********************************/
    private fun writeWavHeader(file: FileOutputStream?, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val header = ByteArray(44)
        val byteRate = sampleRate * channels * bitsPerSample / 8

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[20] = 1 // AudioFormat (1 for PCM)
        header[22] = channels.toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
        header[34] = bitsPerSample.toByte()

        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        //val outputStream = FileOutputStream(file)
        file?.write(header)
        //outputStream.close()
    }

    fun updateWavHeader(outputStream: FileOutputStream, fileSize: Int) {
        val dataSize = fileSize - 44

        val headerSize = intToByteArray(fileSize - 8)
        val dataSizeArray = intToByteArray(dataSize)

        outputStream.channel.position(4)
        outputStream.write(headerSize)

        outputStream.channel.position(40)
        outputStream.write(dataSizeArray)
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }

//    fun updateWavHeader(file: File) {
//        val fileSize = file.length().toInt()
//        val dataSize = fileSize - 44
//
//        val raf = RandomAccessFile(file, "rw")
//        raf.seek(4)
//        raf.write(intToByteArray(fileSize - 8))
//
//        raf.seek(40)
//        raf.write(intToByteArray(dataSize))
//        raf.close()
//    }
}
