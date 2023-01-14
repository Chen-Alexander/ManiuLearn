package com.example.cameraxdecoderpull

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class SocketLive {
    private val tag = "SocketLive"
    private var socketClient: SocketClient? = null
    var videoFrameListener: FrameDataListener? = null
    var audioFrameListener: FrameDataListener? = null

    fun start() {
        socketClient = SocketClient(URI.create("ws://192.168.0.100:59880"), videoFrameListener,
            audioFrameListener)
        socketClient?.connect()
    }

    fun dispose() {
        videoFrameListener = null
        audioFrameListener = null
        socketClient?.close()
    }
}

class SocketClient(
    serverUri: URI,
    private val videoFrameListener: FrameDataListener?,
    private val audioFrameListener: FrameDataListener?
) : WebSocketClient(serverUri) {
    private val tag = "SocketClient"

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(tag, "SocketClient onOpen")
    }

    override fun onMessage(message: String?) {
    }

    override fun onMessage(bytes: ByteBuffer?) {
        super.onMessage(bytes)
        bytes?.let {
            val byteArray = ByteArray(it.remaining())
            bytes.get(byteArray)
            when (byteArray[0]) {
                FrameDataType.Audio.type -> {
                    audioFrameListener?.onFrame(byteArray, FrameDataType.Audio, 1)
                }
                FrameDataType.Video.type -> {
                    videoFrameListener?.onFrame(byteArray, FrameDataType.Video, 1)
                }
                else -> {}
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.i(tag, "SocketClient onClose")
    }

    override fun onError(ex: Exception?) {
        Log.i(tag, "SocketClient onError:${ex?.message}")
    }
}

interface FrameDataListener {
    fun onFrame(byteArray: ByteArray, type: FrameDataType, offset: Int)
}

enum class FrameDataType(val type: Byte) {
    Audio(0),
    Video(1);
}

