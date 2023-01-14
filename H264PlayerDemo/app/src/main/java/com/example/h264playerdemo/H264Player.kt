package com.example.h264playerdemo

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.*
import java.lang.Exception
import kotlin.experimental.and

class H264Player(
    private val surface: Surface,
    private val filePath: String
) : Runnable {
    private val tag = "H264Player"
    private var mediaCodec: MediaCodec? = null

    init {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc")
            val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                0, 0)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
            // 把参数设置给mediaCodec(即交给dsp)
            mediaCodec?.configure(mediaFormat, surface, null, 0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun play() {
        mediaCodec?.start()
        Thread(this).start()
    }

    override fun run() {
        try {
            val bytes = getBytes()
            val bufferInfo = MediaCodec.BufferInfo()
            // 当前编码所使用的缓冲区的索引
            var startIndex = 0
            while (true) {
                // + 2 防止只循环第一帧
                val nextFrameStart = findFrameIndex(bytes, startIndex + 2, bytes.size)
//                Log.i(tag, "inputBuffer数量：${mediaCodec?.inputBuffers?.size}")
//                Log.i(tag, "OutputBuffer数量：${mediaCodec?.outputBuffers?.size}")
                // 尝试获取一个有效输入缓冲区的索引；timeout: 10ms
                val start = System.nanoTime()
                val inIndex = mediaCodec?.dequeueInputBuffer(10 * 1000)
                if (inIndex != null && inIndex > -1) {
                    // 获取到一个有效地输入缓冲区并写入数据
                    val inputBuffer = mediaCodec?.getInputBuffer(inIndex)
                    val len = nextFrameStart - startIndex
                    inputBuffer?.put(bytes, startIndex, len)
                    // 给mediacodec输入数据
                    mediaCodec?.queueInputBuffer(inIndex, 0, len, 0, 0)
                }
                // 尝试获取解码后的数据，还是通过获取索引来获取数据地
                var outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10 * 1000)
                while (outIndex != null && outIndex > -1) {
                    Log.i(tag, "解码时间：${System.nanoTime() - start}")
                    mediaCodec?.releaseOutputBuffer(outIndex, true)
                    outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10 * 1000)
                }
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                // 下一帧的起始位置
                startIndex = nextFrameStart
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 找出下一帧的起始位置
     */
    private fun findFrameIndex(bytes: ByteArray, start: Int, totalSize: Int): Int {
        for (index in start..totalSize - 4) {
            val byt = bytes[index]
//            Log.e(tag, "index值：$byt")
            // TODO 此处有错误
            val iDR = (bytes[index].compareTo(0x00) == 0
                    && bytes[index + 1].compareTo(0x00) == 0
                    && bytes[index + 2].compareTo(0x00) == 0
                    && bytes[index + 3].compareTo(0x01) == 0
                    && ((bytes[index + 4] and 0x7E).toInt() shr 1) == 32)
                    || (bytes[index].compareTo(0x00) == 0
                    && bytes[index + 1].compareTo(0x00) == 0
                    && bytes[index + 2].compareTo(0x01) == 0)
                    && ((bytes[index + 4] and 0x7E).toInt() shr 1) == 32
            val start = (bytes[index].compareTo(0x00) == 0
                    && bytes[index + 1].compareTo(0x00) == 0
                    && bytes[index + 2].compareTo(0x00) == 0
                    && bytes[index + 3].compareTo(0x01) == 0)
                    || (bytes[index].compareTo(0x00) == 0
                    && bytes[index + 1].compareTo(0x00) == 0
                    && bytes[index + 2].compareTo(0x01) == 0)
            if (start) {
                return index
            }
        }
        return -1
    }

    private fun getBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val inputStream = DataInputStream(FileInputStream(filePath))
        val size = 1024
        var buf = ByteArray(size)
        var len: Int = inputStream.read(buf, 0, size)
        while (len > 0) {
            outputStream.write(buf, 0, len)
            len = inputStream.read(buf)
        }
        return outputStream.toByteArray()
    }

    fun dispose() {
        mediaCodec?.release()
        mediaCodec = null
    }
}