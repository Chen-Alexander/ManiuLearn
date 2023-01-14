package com.example.h264encoderdemo

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import java.io.IOException

class H264Encoder(
    private val mediaProjection: MediaProjection,
    private var width: Int,
    private var height: Int,
    private val dpi: Int
) : Runnable {
    private val tag = "H264Encoder"
    private var mediaCodec: MediaCodec? = null

    fun init() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            val mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width,
                height
            )
            val fps = 20
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // GOP大小
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30)
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3 / 2 * fps)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
            // 颜色格式为surface，因为我们是通过surface获取数据的
            mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            // 此处第二个参数只在解码时需要传，现在是编码，不需要渲染，所以不需要传
            // flag: CONFIGURE_FLAG_ENCODE   什么意思？
            mediaCodec?.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE)
            // surface: the input of mediaCode-Encoder, the output of mediaProjection
            val inputSurface = mediaCodec?.createInputSurface()
            mediaProjection.createVirtualDisplay(
                "test", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                inputSurface, null, null
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun launch() {
        Thread(this).start()
    }

    override fun run() {
        mediaCodec?.start()
        // 此处不需要再通过inputBuffer给mediaCodec喂数据，mediaCodec会自动检查inputSurface中的
        // 数据进行编码，此处只需要通过outputBuffer拿编码后的输出数据即可
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10 * 1000)
            if (outputIndex != null && outputIndex > -1) {
                val byteBuffer = mediaCodec?.getOutputBuffer(outputIndex)
                val remain = byteBuffer?.remaining()
                val capacity = byteBuffer?.capacity()
                val bufferSize = bufferInfo.size
                // 把byteBuffer内的数据转移出来
                val byteArray = ByteArray(bufferInfo.size)
                byteBuffer?.get(byteArray)
                FileUtils.writeBytes(byteArray)
                FileUtils.writeContent(byteArray)
                mediaCodec?.releaseOutputBuffer(outputIndex, false)
            }
        }
    }

    fun dispose() {
        mediaCodec?.release()
        mediaCodec = null
    }
}