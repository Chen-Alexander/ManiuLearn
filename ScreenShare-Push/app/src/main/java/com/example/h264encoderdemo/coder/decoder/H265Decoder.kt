package com.example.h264playerdemo

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.io.*

class H265Decoder(
    surface: Surface
) : ScreenShareFrameListener {
    private val tag = "H264Player"
    private var mediaCodec: MediaCodec? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null

    init {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/hevc")
            // 此处宽高传什么无所谓，只要流数据中包含vps/sps/pps数据即可
            val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,
                1080, 2120)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            // 把参数设置给mediaCodec(即交给dsp)
            mediaCodec?.configure(mediaFormat, surface, null, 0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun play() {
        mediaCodec?.start()
    }

    fun dispose() {
        mediaCodec?.release()
        mediaCodec = null
    }

    override fun onFrame(byteArray: ByteArray) {
        if (bufferInfo == null) {
            bufferInfo = MediaCodec.BufferInfo()
        }
        // 尝试获取一个有效输入缓冲区的索引；timeout: 10ms
        val inIndex = mediaCodec?.dequeueInputBuffer(10 * 1000)
        if (inIndex != null && inIndex > -1) {
            // 获取到一个有效地输入缓冲区并写入数据
            val inputBuffer = mediaCodec?.getInputBuffer(inIndex)
            inputBuffer?.put(byteArray, 0, byteArray.size)
            // 给mediacodec输入数据
            mediaCodec?.queueInputBuffer(inIndex, 0, byteArray.size, 0, 0)
        }
        // 尝试获取解码后的数据，还是通过获取索引来获取数据地(某一帧数据经过解码后，数据量会变大，有可能会分多次输出，
        // 所以此处需要通过循环来取数据)
        var outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo!!, 10 * 1000)
        while (outIndex != null && outIndex > -1) {
            mediaCodec?.releaseOutputBuffer(outIndex, true)
            outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo!!, 10 * 1000)
        }
    }
}