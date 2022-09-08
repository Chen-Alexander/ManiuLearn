package com.example.cameraxdecoderpull

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioTrack.MODE_STREAM
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.ActivityCompat
import java.lang.RuntimeException

class AudioPlayerLive(context: Context) : FrameDataListener {
    companion object {
        const val TAG = "AudioRecorder"
    }

    private var audioTrack: AudioTrack? = null

    // 音乐类型， 使用扬声器播放
    val streamType = AudioManager.STREAM_MUSIC
    val mode = MODE_STREAM

    // 采样率
    private val sampleRate = 44100

    // 声道数  MONO 单声道   STEREO 多声道
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO

    // 每次采样的数据大小  ENCODING_PCM_8BIT ENCODING_PCM_FLOAT
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize: Int =
        AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // 单独线程去进行音频录制
    private val handlerThread = HandlerThread("audio-record")
    private var handler: Handler? = null
    private var isRecording = false
    var frameDataListener: FrameDataListener? = null

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("not have enough permission!")
        }
        audioTrack = AudioTrack(
            streamType, sampleRate, channelConfig, audioFormat, minBufferSize,
            mode
        )
        // 设置音量大小
        audioTrack?.setVolume(16F)
        audioTrack?.play()
    }

    fun dispose() {
        isRecording = false
        handler?.removeCallbacksAndMessages(null)
        handler = null
        audioTrack?.release()
        handler = null
        frameDataListener = null
    }

    override fun onFrame(byteArray: ByteArray, type: FrameDataType, offset: Int) {
        if (type != FrameDataType.Audio) {
            return
        }
        val ret = audioTrack?.write(byteArray, offset, byteArray.size - offset)
        Log.i(TAG, "播放返回值:$ret")
        when (ret) {
            AudioTrack.ERROR_INVALID_OPERATION,
            AudioTrack.ERROR_BAD_VALUE,
            AudioTrack.ERROR_DEAD_OBJECT -> {
                Log.i(TAG, "播放失败")
                return
            }
            else -> {}
        }
        Log.i(TAG, "播放成功")
    }
}