package com.example.h264encoderdemo.coder.encoder

import android.media.MediaCodec
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.example.h264encoderdemo.queue.MediaBufferQueue
import com.example.h264encoderdemo.util.FileUtils
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.math.roundToInt

abstract class VideoEncoder(
    protected var width: Int,
    protected var height: Int,
    protected val fps: Int,
    protected val gopSize: Int,
    private val queue: MediaBufferQueue
) : Encoder {
    open val tag = "VideoEncoder"
    private val sPSNalu = 7
    private val iNalu = 5
    protected var mediaCodec: MediaCodec? = null

    // 用于记录强制输出I帧的时间
    private var forceIFrameTS = currentTimeMillis()
    private val forIFrameBundle: Bundle by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Bundle().apply {
            //立即刷新 让下一帧是关键帧
            this.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
        }
    }

    @Volatile
    private var disposed = false
    private var sPSPPS: ByteArray? = null

    override fun createInputSurface(): Surface? {
        return mediaCodec?.createInputSurface()
    }

    override fun launch() {
        Thread(this).start()
    }

    override fun run() {
        mediaCodec?.start()
        // 初始化forceIFrameTS
        forceIFrameTS = currentTimeMillis()
        // 此处不需要再通过inputBuffer给mediaCodec喂数据，mediaCodec会自动检查inputSurface中的
        // 数据进行编码，此处只需要通过outputBuffer拿编码后的输出数据即可
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            Log.i(tag, "disposed is $disposed")
            if (disposed) {
                break
            }
            // 在合适的时间强制输出关键帧
            if (isForceIFrameTime(forceIFrameTS)) {
                mediaCodec?.setParameters(forIFrameBundle)
                forceIFrameTS = currentTimeMillis()
            }
            Log.i(tag, "ready execute dequeueOutputBuffer.")
            val outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10 * 1000)
            Log.i(tag, "dequeueOutputBuffer outputIndex is $outputIndex.")
            if (outputIndex != null && outputIndex > -1) {
                val byteBuffer = mediaCodec?.getOutputBuffer(outputIndex)
                // 把byteBuffer内的数据转移出来
                byteBuffer?.let { buffer ->
                    val index = dealFrame(buffer, bufferInfo)
                    index?.let {
                        enqueueBuf(it)
                    }
                }
                mediaCodec?.releaseOutputBuffer(outputIndex, false)
            }
        }
        // dispose mediaCodec
        mediaCodec?.release()
        mediaCodec = null
    }

    private fun isForceIFrameTime(curTS: Long): Boolean {
        val interval = ((gopSize.toFloat() * 2.0F) / fps.toFloat()).roundToInt() * 1000L
        val dif = currentTimeMillis() - curTS
        return dif > interval
    }

    private fun dealFrame(byteBuffer: ByteBuffer, bufInfo: MediaCodec.BufferInfo): Int? {
        var offset = 4
        if (byteBuffer[2].compareTo(0x01) == 0) {
            offset = 3
        }
        // 通过第五个字节获取NALU类型
        when ((byteBuffer[offset] and 0x1F).toInt()) {
            sPSNalu -> {
                // 缓存SPS/PPS帧
                sPSPPS = ByteArray(bufInfo.size)
                byteBuffer.get(sPSPPS, 0, bufInfo.size)
            }
            iNalu -> {
                sPSPPS?.let {
                    // 每个IDR帧前边都加上SPS/PPS
                    val pair = dequeueBuf(it.size + bufInfo.size)
                    pair?.let { _ ->
                        System.arraycopy(it, 0, pair.second, 0, it.size)
                        byteBuffer.get(pair.second, it.size, bufInfo.size)
                        return pair.first
                    }
                }
            }
            else -> {
                // 其他帧类型直接发送
                val pair = dequeueBuf(bufInfo.size)
                pair?.let { _ ->
                    kotlin.runCatching {
                        byteBuffer.get(pair.second, 0, bufInfo.size)
                    }.onFailure {
                        it.printStackTrace()
                    }
                    return pair.first
                }
            }
        }
        return null
    }

    private fun dequeueBuf(size: Int): Pair<Int, ByteArray>? {
        val index = queue.dequeue(size)
        if (index > -1) {
            val tmp = queue.getBuf(index)
            return Pair(index, tmp.buf)
        }
        Log.e(tag, "未获取到可用缓冲区!")
        // 获取下标小于0，说明现在无可用缓冲区(当然也可以死循环获取)
        return null
    }

    private fun enqueueBuf(index: Int) {
        // for debug
        writeFile(queue.getBuf(index).buf)
        queue.enqueue(index)
    }

    private fun writeFile(byteArray: ByteArray) {
        FileUtils.writeBytes(byteArray)
    }

    override fun dispose() {
        Log.d(tag, "dispose() called")
        disposed = true
    }
}