package com.example.cameraxencoderpush

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

class H264Encoder(
    private var width: Int,
    private var height: Int
) {
    private val tag = "H264Encoder"
    private var mediaCodec: MediaCodec? = null
    private val fps = 30
    private lateinit var bufferInfo: MediaCodec.BufferInfo
    private var frameIndex = 0
    // microSeconds
    private val timeout = 10 * 1000L
    var frameDataListener: FrameDataListener? = null

    private val sPSNalu = 7
    private val iNalu = 5
    private var sPSPPS: ByteArray? = null

    fun init() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            val mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                width,
                height
            )
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // GOP大小
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3 / 2 * fps)
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
            mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            // 此处第二个参数只在解码时需要传，现在是编码，不需要渲染，所以不需要传
            // flag: CONFIGURE_FLAG_ENCODE   什么意思？
            mediaCodec?.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun encodeFrame(frame: ByteArray) {
        if (!::bufferInfo.isInitialized) {
            bufferInfo = MediaCodec.BufferInfo()
        }
        mediaCodec?.let { codec ->
            val inputBufIndex = codec.dequeueInputBuffer(timeout)
            if (inputBufIndex > -1) {
                val buffer = codec.getInputBuffer(inputBufIndex)
                buffer?.clear()
                buffer?.put(frame)
                val pts = computePts()
                Log.e(tag, "pts: $pts")
                codec.queueInputBuffer(inputBufIndex, 0, frame.size, pts, 0)
                frameIndex++
            }
            var outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
            while (outputBufIndex > -1) {
                val outputBuffer = codec.getOutputBuffer(outputBufIndex)
                val byteArray = outputBuffer?.let { dealFrame(outputBuffer, bufferInfo) }
                byteArray?.let {
                    frameDataListener?.onFrame(it, FrameDataType.Video)
                    FileUtils.writeVideoBytes(it)
                }
                codec.releaseOutputBuffer(outputBufIndex, false)
                outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
            }
        }
    }

    private fun computePts(): Long {
        // 单位微秒
        return 1000 * 1000L / fps * frameIndex
    }

    fun dispose() {
        mediaCodec?.release()
        mediaCodec = null
    }

    /**
     * 在每一个关键帧前边加上SEI/SPS/PPS
     * */
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
                byteBuffer.get(sPSPPS!!)
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
}

interface FrameDataListener {
    /**
     * @param type 0: audio  1: video
     * */
    fun onFrame(data: ByteArray, type: FrameDataType)
}

enum class FrameDataType(val type: Byte) {
    Audio(0),
    Video(1);
}