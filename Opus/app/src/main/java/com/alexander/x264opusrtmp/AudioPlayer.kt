package com.alexander.x264opusrtmp

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat.*
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STREAM
import android.util.Log
import com.alexander.x264opusrtmp.Constants.opusSampleRate
import com.alexander.x264opusrtmp.util.Utils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * opus编解码注意点：40ms的数据，frameSize是640字节，输入编码器的是ShortArray，大小为320，输出的编码后数据为字节数组，
 * 大小为40，即缩小了八倍；同理，解码时，解码器接收的是字节数组，大小为40，解码器输出的是ShortArray，大小为320；
 * */
class AudioPlayer(
    context: Context,
    private val livePusher: LivePusher
) : Runnable {
    private val tag = "AudioOpusEncodeChannel"

    @Volatile
    private var disposed = false
    private val channelCount = Constants.channelCount
    private val channelConfig = Constants.outChannelConfig

    // 输入信号的采样率(Hz)，必须是8000、12000、16000、24000、或48000
    /** 帧长约束：
     * opus为了对一个帧进行编码，必须正确地用音频数据的帧(2.5, 5, 10, 20, 40 or 60 ms)
     * 来调用opus_encode()或opus_encode_float()函数。
     * 比如，在48kHz的采样率下，opus_encode()参数中的合法的frame_size（单通道的帧大小）值只有：
     * 120, 240, 480, 960, 1920, 2880。即：
     *    frame_size = 采样率 * 帧时间 / 1000，例如16000 * 10 / 1000即表示在10ms内的采样次数
     * 因为需要满足帧时间长度为10,20,40,60ms这些才能编码opus，因而需要对输入数据进行缓冲裁剪 */
    private val frameSize = opusSampleRate * channelCount * 20 / 1000
    private var opusFilePath: String? = null
    private val bufSize = AudioTrack.getMinBufferSize(opusSampleRate, channelConfig, ENCODING_PCM_16BIT)
    private val audioAttributes =
        AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build()
    private val audioFormat = Builder().setEncoding(ENCODING_PCM_16BIT)
        .setSampleRate(opusSampleRate).setChannelMask(channelConfig)
        .build()
    private var audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val sessionId = audioManager?.generateAudioSessionId()
    private var audioTrack: AudioTrack? = null
    private var filePcmOutputStream: FileOutputStream? = null
    private var filePcmBufferedOutputStream: BufferedOutputStream? = null

    @SuppressLint("MissingPermission")
    fun init(opusFilePath: String) {
        runCatching {
            sessionId?.let {
                audioTrack = AudioTrack(audioAttributes, audioFormat, bufSize, MODE_STREAM, it)
            }
            this.opusFilePath = opusFilePath
            Log.e(tag, "opusFilePath:$opusFilePath")
            val opusFile = File(opusFilePath)
            val name = opusFile.name.split(".")[0].plus("_decoded.pcm")
            val decodedPcmPath = opusFile.parentFile?.absolutePath.plus("/").plus(name)
            Log.e(tag, "decodedPcmPath:$decodedPcmPath")
            val pcmFile = File(decodedPcmPath)
            if (pcmFile.parentFile?.exists() == false) {
                pcmFile.parentFile?.mkdirs()
            }
            if (pcmFile.exists()) {
                pcmFile.delete()
            }
            pcmFile.createNewFile()
            filePcmOutputStream = FileOutputStream(pcmFile)
            filePcmBufferedOutputStream = BufferedOutputStream(filePcmOutputStream)
            livePusher.native_setOpusDecInfo(opusSampleRate, channelCount)
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun launch() {
        Thread(this).start()
    }

    fun dispose() {
        Log.d(tag, "dispose() called")
        if (!disposed) {
            disposed = true
            audioTrack?.stop()
            audioTrack?.release()
            audioManager = null
            if (filePcmBufferedOutputStream != null) {
                filePcmBufferedOutputStream!!.close()
                filePcmBufferedOutputStream = null
            }
            if (filePcmOutputStream != null) {
                filePcmOutputStream!!.close()

                filePcmOutputStream = null
            }
        }
    }

    override fun run() {
        audioTrack?.play()
        disposed = false
        val fis: FileInputStream
        try {
            fis = FileInputStream(opusFilePath)
        } catch (e: Exception) {
            e.printStackTrace()
            dispose()
            return
        }
        val bis = BufferedInputStream(fis)
        while (true) {
            val bufArray = ByteArray(frameSize / 2 / 8)
            var read = -1
            try {
                read = bis.read(bufArray, 0, bufArray.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (read < 0) {
                Log.e(tag, "decode finish.")
                break
            } else {
                // 解码后是编码的八倍，但是此处是short，所以除以2
                val decodedBufArray = ShortArray(frameSize / 2)
                val size = livePusher.native_OpusDecode(bufArray, decodedBufArray)
                if (size > 0) {
                    val decodedArray = ShortArray(size)
                    System.arraycopy(decodedBufArray, 0, decodedArray, 0, size)
                    // 解码完成，播放并保存
                    audioTrack?.write(decodedArray, 0, size)
                    filePcmBufferedOutputStream?.write(Utils.shortArrayToByteArray(decodedArray))
                } else {
                    Log.e(tag, "decode error!")
                }
            }
        }
        bis.close()
        fis.close()
        if (!disposed) {
            dispose()
        }
    }
}






















