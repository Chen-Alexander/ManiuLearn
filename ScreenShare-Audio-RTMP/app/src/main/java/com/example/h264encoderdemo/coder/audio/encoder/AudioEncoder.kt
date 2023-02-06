package com.example.h264encoderdemo.coder.audio.encoder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Log
import com.example.h264encoderdemo.beans.RTMPPacket
import com.example.h264encoderdemo.beans.RTMPPacketType
import com.example.h264encoderdemo.queue.MediaBufferQueue
import com.example.h264encoderdemo.util.FileUtils
import java.lang.System.nanoTime
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class AudioEncoder(
    private val queue: LinkedBlockingQueue<RTMPPacket?>
) : Encoder {
    private val tag = "AudioEncoder"
    private var mediaCodec: MediaCodec? = null
    private val timeout = 300L
    private var minBufSize = 0

    @Volatile
    private var disposed = false
    private var audioRecord: AudioRecord? = null
    private var startTime = 0L
    private val channelCount = 1
    private val sampleRate = 44100
    private val bitRate = 128_000
    private val maxInputSize = 10 * 1024

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
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
            // 设置pcm帧的最大size(输入大小)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)
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
        // 首先发送audio special info，即audio_head
        val audioHeadPacket = RTMPPacket(RTMPPacketType.RTMP_PACKET_TYPE_AUDIO_HEAD.type)
        audioHeadPacket.buffer = byteArrayOf(0x12, 0x08)
        // 添加到缓存队列
        queue.offer(audioHeadPacket)

        // pcm音频数据存放缓冲区
        val pcmBuf = ByteArray(minBufSize)
        while (!disposed) {
            // 从audioRecord中读取原始pcm数据
            val pcmLen = audioRecord?.read(pcmBuf, 0, pcmBuf.size) ?: 0
            if (pcmLen <= 0) {
                // 没有读取到数据，继续重试
                continue
            }
            // 编码pcm为aac数据
            mediaCodec?.let { codec ->
                // 获取输入缓冲区
                val inputIndex = codec.dequeueInputBuffer(timeout)
                if (inputIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inputIndex)
                    inputBuf?.clear()
                    inputBuf?.put(pcmBuf, 0, pcmLen)
                    val pts = nanoTime() / 1000
                    // 数据进队，等待被编码
                    codec.queueInputBuffer(inputIndex, 0, pcmLen, pts, 0)
                }
                // 尝试获取编码后的aac数据
                var outputIndex = codec.dequeueOutputBuffer(bufInfo, timeout)
                while (outputIndex >= 0 && !disposed) {
                    val outputBuf = codec.getOutputBuffer(outputIndex)
                    // 获取到编码数据后初始化startTime，是为了使显示时间更精确
                    if (startTime == 0L) {
                        startTime = bufInfo.presentationTimeUs
                    }
                    val outData = ByteArray(bufInfo.size)
                    outputBuf?.get(outData)
                    writeFile(outData)
                    val audioDataPacket = RTMPPacket(
                        RTMPPacketType.RTMP_PACKET_TYPE_AUDIO_DATA.type,
                        (bufInfo.presentationTimeUs - startTime) / 1000L
                    )
                    audioDataPacket.buffer = outData
                    // 添加到缓存队列
                    queue.offer(audioDataPacket)
                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(bufInfo, timeout)
                }
            }
        }
        // dispose mediaCodec
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null
    }

    private fun writeFile(byteArray: ByteArray) {
        FileUtils.writeBytes(byteArray)
    }
}























