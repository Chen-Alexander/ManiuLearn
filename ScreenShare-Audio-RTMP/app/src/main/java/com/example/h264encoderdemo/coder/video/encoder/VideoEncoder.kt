package com.example.h264encoderdemo.coder.video.encoder

import android.media.MediaCodec
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.example.h264encoderdemo.beans.RTMPPacket
import com.example.h264encoderdemo.beans.RTMPPacketType
import com.example.h264encoderdemo.util.FileUtils
import java.lang.System.currentTimeMillis
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.roundToInt

abstract class VideoEncoder(
    protected var width: Int,
    protected var height: Int,
    protected val fps: Int,
    protected val gopSize: Int,
    private val queue: LinkedBlockingQueue<RTMPPacket?>
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
    private var startTime = 0L
    private val timeout = 300L

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
//            Log.i(tag, "disposed is $disposed")
            if (disposed) {
                break
            }
            // 在合适的时间强制输出关键帧
            if (isForceIFrameTime(forceIFrameTS)) {
                mediaCodec?.setParameters(forIFrameBundle)
                forceIFrameTS = currentTimeMillis()
            }
//            Log.i(tag, "ready execute dequeueOutputBuffer.")
            var outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, timeout)
//            Log.i(tag, "dequeueOutputBuffer outputIndex is $outputIndex.")
            while (outputIndex != null && outputIndex > -1) {
                val outputBuf = mediaCodec?.getOutputBuffer(outputIndex)
                // 获取到编码数据后初始化startTime，是为了使显示时间更精确
                if (startTime == 0L) {
                    startTime = bufferInfo.presentationTimeUs
                }
                val outData = ByteArray(bufferInfo.size)
                outputBuf?.get(outData)
                writeFile(outData)
                val videoDataPacket = RTMPPacket(
                    RTMPPacketType.RTMP_PACKET_TYPE_VIDEO.type,
                    (bufferInfo.presentationTimeUs - startTime) / 1000L
                )
                videoDataPacket.buffer = outData
                // 添加到缓存队列
                queue.offer(videoDataPacket)
                mediaCodec?.releaseOutputBuffer(outputIndex, false)
                outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, timeout)
            }
        }
        // dispose mediaCodec
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null
    }

    private fun isForceIFrameTime(curTS: Long): Boolean {
        val interval = ((gopSize.toFloat() * 2.0F) / fps.toFloat()).roundToInt() * 1000L
        val dif = currentTimeMillis() - curTS
        return dif > interval
    }

    private fun writeFile(byteArray: ByteArray) {
        FileUtils.writeBytes(byteArray)
    }

    override fun dispose() {
        Log.d(tag, "dispose() called")
        disposed = true
    }
}