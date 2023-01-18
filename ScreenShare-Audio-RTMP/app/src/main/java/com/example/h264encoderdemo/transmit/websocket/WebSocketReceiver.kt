package com.example.h264encoderdemo.transmit.websocket

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class Receiver(private val address: String) {
    private val tag = "Receiver"
    private var socketClient: SocketClient? = null
    var listener: ScreenShareFrameListener? = null

    fun start() {
        socketClient = SocketClient(URI.create("ws://".plus(address)), listener)
        socketClient?.connect()
    }

    fun dispose() {
        listener = null
        socketClient?.close()
    }
}

class SocketClient(serverUri: URI, private val listener: ScreenShareFrameListener?) : WebSocketClient(serverUri) {
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
            listener?.onFrame(byteArray)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.i(tag, "SocketClient onClose")
    }

    override fun onError(ex: Exception?) {
        Log.i(tag, "SocketClient onError:${ex?.message}")
    }
}

interface ScreenShareFrameListener {
    fun onFrame(byteArray: ByteArray)
}

