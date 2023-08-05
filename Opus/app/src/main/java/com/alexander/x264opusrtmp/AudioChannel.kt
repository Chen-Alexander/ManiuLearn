package com.alexander.x264opusrtmp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.util.Log
import com.alexander.x264opusrtmp.Constants.inChannelConfig
import com.alexander.x264opusrtmp.Constants.channelCount
import com.alexander.x264opusrtmp.Constants.opusEncodeBitRate
import com.alexander.x264opusrtmp.Constants.opusSampleRate
import com.alexander.x264opusrtmp.util.AudioFileUtils
import com.alexander.x264opusrtmp.util.Utils.byteArrayToShortArray

class AudioChannel(
    private val livePusher: LivePusher
) : Runnable {
    private val tag = "AudioOpusEncodeChannel"

    @Volatile
    private var disposed = false
    private var minBufSize = 0
    private var audioRecord: AudioRecord? = null
    // 输入信号的采样率(Hz)，必须是8000、12000、16000、24000、或48000
    /** 帧长约束：
     * opus为了对一个帧进行编码，必须正确地用音频数据的帧(2.5, 5, 10, 20, 40 or 60 ms)
     * 来调用opus_encode()或opus_encode_float()函数。
     * 比如，在48kHz的采样率下，opus_encode()参数中的合法的frame_size（单通道的帧大小）值只有：
     * 120, 240, 480, 960, 1920, 2880。即：
     *    frame_size = 采样率 * 帧时间 / 1000，例如16000 * 10 / 1000即表示在20ms内的采样次数
     * 因为需要满足帧时间长度为10,20,40,60ms这些才能编码opus，因而需要对输入数据进行缓冲裁剪 */
    private val bytesPerTenMS = opusSampleRate * channelCount * 20 / 1000
    private var mRemainBuf: ByteArray? = null
    private var mRemainSize = 0
    private var audioBuffer: ByteArray? = null
    private var debugMode = true

    @SuppressLint("MissingPermission")
    fun init(debugFilePath: String) {
        runCatching {
            // 初始化audioRecord
            minBufSize = AudioRecord.getMinBufferSize(
                opusSampleRate,
                inChannelConfig,
                AudioFormat.ENCODING_PCM_16BIT
            ) + 2048
            audioRecord = AudioRecord(
                AudioSource.MIC,
                opusSampleRate,
                inChannelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize
            )
            audioBuffer = ByteArray(minBufSize)
            mRemainBuf = ByteArray(bytesPerTenMS)
            // opus编码只支持以下几个采样率8k、12k、16k、24k、48k
            livePusher.native_setOpusEncInfo(opusSampleRate, channelCount, opusEncodeBitRate, 3, debugFilePath)
//            livePusher.native_setAACAudioEncInfo(sampleRate, channelCount, debugFilePath)
            if (debugMode) {
                AudioFileUtils.initAudioFile()
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun launch() {
        Thread(this).start()
    }

    fun dispose() {
        Log.d(tag, "dispose() called")
        disposed = true
    }

    /**
     * read出的数据不一定是一帧(bytesPerTenMS)，所以这里当数据小于一帧bytesPerTenMS时，需等待后续数据，
     * 当大于一帧时，需要拆分。
     * */
    override fun run() {
        audioRecord?.startRecording()
        audioBuffer = ByteArray(minBufSize)
        while (!disposed) {
            // 读取原始pcm数据
            audioBuffer?.let {
                var pcmLen = audioRecord?.read(it, 0, minBufSize) ?: 0
                if (debugMode) {
//                    AudioFileUtils.writeAudioBytes(it)
                }
                if (pcmLen <= 0) {
                    // 继续读取
                    return@let
                }
                var data = it
                if (mRemainSize > 0) {
                    val totalBuf = ByteArray(pcmLen + mRemainSize)
                    System.arraycopy(mRemainBuf, 0, totalBuf, 0, mRemainSize)
                    System.arraycopy(data, 0, totalBuf, mRemainSize, pcmLen)
                    data = totalBuf
                    pcmLen += mRemainSize
                    mRemainSize = 0
                }
                var hasHandleSize = 0
                while (hasHandleSize < pcmLen) {
                    val readCount = bytesPerTenMS
                    if (bytesPerTenMS > pcmLen) {
                        Log.i(tag, "bytesPerTenMs > pcmLen")
                        mRemainSize = pcmLen
                        System.arraycopy(data, 0, mRemainBuf, 0, pcmLen)
                        break
                    }
                    if ((pcmLen - hasHandleSize) < readCount) {
                        mRemainSize = pcmLen - hasHandleSize
                        Log.d(tag, "remain size :$mRemainSize")
                        System.arraycopy(data, hasHandleSize, mRemainBuf, 0, mRemainSize)
                        break
                    }
                    val bytes = ByteArray(readCount)
                    System.arraycopy(data, hasHandleSize, bytes, 0, readCount)
                    val shortArray = byteArrayToShortArray(bytes)
                    livePusher.native_pushOpusAudio(shortArray)
//                    livePusher.native_pushAACAudio(bytes)
                    hasHandleSize += readCount
                }
            }
        }
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        if (debugMode) {
            AudioFileUtils.release()
        }
    }



}






















