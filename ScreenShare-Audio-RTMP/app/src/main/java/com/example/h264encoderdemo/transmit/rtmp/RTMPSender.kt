package com.example.h264encoderdemo.transmit.rtmp

import android.util.Log
import com.example.h264encoderdemo.beans.RTMPPacket
import java.util.concurrent.LinkedBlockingQueue

class RTMPSender(
    private val url: String,
    private val queue: LinkedBlockingQueue<RTMPPacket>
) : Runnable {
    private val tag = "RTMPSender"

    @Volatile
    private var disposed = false

    companion object {
        init {
            System.loadLibrary("rtmp-sender")
        }
    }

    fun launch() {
        Thread(this, tag).start()
    }

    fun dispose() {
        disposed = true
    }

    override fun run() {
        val connected = connect(url)
        if (!connected) {

            Log.e(tag, "连接失败！")
            return
        }
        while (!disposed) {
            val rtmpPacket = queue.poll()
            Log.i(tag, "取出数据")
            if (rtmpPacket.buffer != null && rtmpPacket.buffer!!.isNotEmpty()) {
                Log.i(tag, "推送数据大小：${rtmpPacket.buffer!!.size}")
                val result = sendData(
                    rtmpPacket.buffer!!, rtmpPacket.buffer!!.size, rtmpPacket.tms,
                    rtmpPacket.type
                )
                val msg = if (result) "推送成功" else "推送失败"
                Log.i(tag, msg)
            }
        }
    }

    private external fun connect(url: String): Boolean
    private external fun sendData(data: ByteArray, len: Int, tms: Long, type: Int): Boolean
}