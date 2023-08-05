package com.alexander.x264opusrtmp

import android.util.Log
import com.alexander.x264opusrtmp.util.FileUtils

class LivePusher() {
    private val tag = "LivePusher"
    var audioChannel: AudioChannel? = null

    init {
        native_init(true)
    }

    companion object {
        // Used to load the 'x264opusrtmp' library on application startup.
        init {
            System.loadLibrary("opusaudio")
        }
    }

    private external fun native_init(isDebug: Boolean)

    private external fun native_start(path: String?)

    external fun native_setOpusEncInfo(
        sampleRate: Int,
        channelCount: Int,
        bitRate: Int,
        complexity: Int,
        debugPath: String?
    )

    external fun native_pushOpusAudio(data: ShortArray?)

    external fun native_setAACAudioEncInfo(
        sampleRate: Int,
        channelCount: Int,
        debugPath: String?
    ): Int

    external fun native_pushAACAudio(data: ByteArray?)

    external fun native_stop()

    external fun native_release()

    external fun native_setOpusDecInfo(sampleRate: Int, channelCount: Int)

    external fun native_OpusDecode(encoded: ByteArray, decoded: ShortArray): Int

    //native 层 回调
    private fun postData(data: ByteArray) {
        Log.i(tag, "postData: " + data.size)
        FileUtils.writeBytes(data)
    }

    fun startLive(path: String?) {
        native_start(path)
        audioChannel?.launch()
    }

    fun dispose() {
        audioChannel?.dispose()
        native_stop()
        native_release()
    }
}