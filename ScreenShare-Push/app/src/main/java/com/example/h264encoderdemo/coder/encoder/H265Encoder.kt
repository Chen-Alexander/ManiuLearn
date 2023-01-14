package com.example.h264encoderdemo.coder.encoder

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import com.example.h264encoderdemo.util.FileUtils
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

class H265VideoEncoder(
    private val mediaProjection: MediaProjection,
    private var width: Int,
    private var height: Int,
    private val dpi: Int
) : VideoEncoder {
    private val tag = "H265Encoder"
    private val vPSNalu = 32
    private val iNalu = 19
    private var mediaCodec: MediaCodec? = null

    @Volatile
    private var disposed = false
    var dataListener: ScreenShareDataListener? = null
    private var vPSSPSPPS: ByteArray? = null

    fun init() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/hevc")
            val mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_HEVC, width,
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
        when ((byteBuffer[offset] and 0x7E).toInt() shr 1) {
            vPSNalu -> {
                // 缓存SPS/PPS帧
                vPSSPSPPS = ByteArray(bufferInfo.size)
                byteBuffer.get(vPSSPSPPS)
            }
            iNalu -> {
                vPSSPSPPS?.let {
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

    fun dispose() {
        mediaProjection.stop()
        disposed = true
        mediaCodec?.release()
        mediaCodec = null
    }
}

interface ScreenShareDataListener {
    fun onFrame(data: ByteArray)
}