package com.example.h264encoderdemo.coder.encoder

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.example.h264encoderdemo.util.FileUtils
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

class H264Encoder(
    private var width: Int,
    private var height: Int
) : Encoder {
    private val tag = "H264Encoder"
    private val sPSNalu = 7
    private val iNalu = 5
    private var mediaCodec: MediaCodec? = null

    @Volatile
    private var disposed = false
    var dataListener: ScreenShareDataListener? = null
    private var sPSPPS: ByteArray? = null

    override fun init() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            val mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width,
                height
            )
            val fps = 20
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // GOP大小
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
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

    override fun createInputSurface(): Surface? {
        return mediaCodec?.createInputSurface()
    }

    override fun launch() {
        Thread(this).start()
    }

    override fun run() {
        mediaCodec?.start()
        // 此处不需要再通过inputBuffer给mediaCodec喂数据，mediaCodec会自动检查inputSurface中的
        // 数据进行编码，此处只需要通过outputBuffer拿编码后的输出数据即可
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            if (disposed) {
                break
            }
            val outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10 * 1000)
            if (outputIndex != null && outputIndex > -1) {
                val byteBuffer = mediaCodec?.getOutputBuffer(outputIndex)
                // 把byteBuffer内的数据转移出来
                byteBuffer?.let { buffer ->
                    val byteArray = dealFrame(buffer, bufferInfo)
                    byteArray?.let {
                        dataListener?.onFrame(it)
                        writeFile(it)
                    }
                }
                mediaCodec?.releaseOutputBuffer(outputIndex, false)
            }
        }
    }

    private fun dealFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo): ByteArray? {
        var offset = 4
        if (byteBuffer[2].compareTo(0x01) == 0) {
            offset = 3
        }
        // 通过第五个字节获取NALU类型
        when ((byteBuffer[offset] and 0x1F).toInt()) {
            sPSNalu -> {
                // 缓存SPS/PPS帧
                sPSPPS = ByteArray(bufferInfo.size)
                byteBuffer.get(sPSPPS)
            }
            iNalu -> {
                sPSPPS?.let {
                    // 每个IDR帧前边都加上SPS/PPS
                    val byteArray = ByteArray(bufferInfo.size)
                    byteBuffer.get(byteArray)
                    val newBuf = ByteArray(it.size + bufferInfo.size)
                    System.arraycopy(it, 0, newBuf, 0, it.size)
                    System.arraycopy(byteArray, 0, newBuf, it.size, bufferInfo.size)
                    return newBuf
                }
            }
            else -> {
                // 其他帧类型直接发送
                val byteArray = ByteArray(bufferInfo.size)
                byteBuffer.get(byteArray)
                return byteArray
            }
        }
        return null
    }

    private fun writeFile(byteArray: ByteArray) {
        FileUtils.writeBytes(byteArray)
        FileUtils.writeContent(byteArray)
    }

    override fun dispose() {
        disposed = true
        mediaCodec?.release()
        mediaCodec = null
    }
}