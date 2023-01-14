package com.example.cameraxencoderpush

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import androidx.core.app.ActivityCompat
import java.lang.RuntimeException

class AudioRecorderLive(context: Context) {
    companion object {
        const val TAG = "AudioRecorder"
    }

    private var audioRecord: AudioRecord? = null

    // 采样率
    private val sampleRate = 44100
    // 声道数  MONO 单声道   STEREO 多声道
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    // 每次采样的数据大小  ENCODING_PCM_8BIT ENCODING_PCM_FLOAT
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize: Int =
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    // 单独线程去进行音频录制
    private val handlerThread = HandlerThread("audio-record")
    private var handler:Handler? = null
    private var isRecording = false
    private val audioData: ByteArray
    var frameDataListener: FrameDataListener? = null

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("not have enough permission!")
        }
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
            audioFormat, minBufferSize)
        audioData = ByteArray(minBufferSize)
    }

    fun startRecord() {
        audioRecord?.startRecording()
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        isRecording = true
        handler?.post {
            while (isRecording) {
                val len = audioRecord?.read(audioData, 0, minBufferSize)
                FileUtils.writeAudioBytes(audioData)
                frameDataListener?.onFrame(audioData, FrameDataType.Audio)
            }
        }
    }

    fun dispose() {
        isRecording = false
        handler?.removeCallbacksAndMessages(null)
        handler = null
        audioRecord?.release()
        handler = null
        frameDataListener = null
    }
}