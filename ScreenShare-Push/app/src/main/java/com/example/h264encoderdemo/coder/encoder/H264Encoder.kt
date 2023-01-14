package com.example.h264encoderdemo.coder.encoder

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.example.h264encoderdemo.queue.MediaBufferQueue
import com.example.h264encoderdemo.util.FileUtils
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

class H264Encoder(
    width: Int,
    height: Int,
    fps: Int,
    gopSize: Int,
    queue: MediaBufferQueue
) : VideoEncoder(width, height, fps, gopSize, queue) {
    override val tag = "H264Encoder"

    override fun init() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            val mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width,
                height
            )
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // GOP大小，并不一定是7,当画面无变化时，I帧就不会出现
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, gopSize)
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
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}