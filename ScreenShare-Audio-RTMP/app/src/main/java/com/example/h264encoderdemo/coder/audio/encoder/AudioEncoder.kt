package com.example.h264encoderdemo.coder.audio.encoder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Log

class AudioEncoder : Encoder {
    private val tag = "AudioEncoder"
    private var mediaCodec: MediaCodec? = null
    private var minBufSize = 0

    @Volatile
    private var disposed = false
    private var audioRecord: AudioRecord? = null
    private var startTime = 0L
    private val channelCount = 1
    private val sampleRate = 44100
    private val bitRate = 128_000

    override fun init() {
        runCatching {
            // 设置aac的编码属性
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channelCount
            )
            // 设置aac的质量级别
            format.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            // 设置码率
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // 设置采样率  通道数  采样位数
            minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufSize
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun launch() {
        Thread(this).start()
    }

    override fun dispose() {
        Log.d(tag, "dispose() called")
        disposed = true
    }

    override fun run() {
        mediaCodec?.start()
        audioRecord?.startRecording()
        val bufInfo = MediaCodec.BufferInfo()

    }
}