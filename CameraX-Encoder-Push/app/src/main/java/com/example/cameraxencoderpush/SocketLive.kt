package com.example.cameraxencoderpush

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress

class SocketLive() : FrameDataListener {
    private val tag = "SocketLive"
    private var socket: WebSocket? = null
    private var socketServer: WebSocketServer? = null

    init {
        socketServer = object : WebSocketServer(InetSocketAddress(59880)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Log.i(tag, "socketServer onOpen")
                this@SocketLive.socket = conn
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                Log.i(tag, "socketServer onClose")
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
                Log.i(tag, "socketServer onError: ${ex?.message}")
                
            }

            override fun onStart() {
                Log.i(tag, "socketServer onStart")
            }
        }
    }

    fun start() {
        socketServer?.start()
    }

    private var videoByteArray: ByteArray? = null
    private lateinit var audioByteArray: ByteArray
    override fun onFrame(data: ByteArray, type: FrameDataType) {
        if (socket?.isOpen != true) {
            Log.e(tag, "socket is not open!")
            return
        }
        when (type) {
            FrameDataType.Audio -> {
                if (!::audioByteArray.isInitialized) {
                    audioByteArray = ByteArray(data.size + 1)
                }
                audioByteArray[0] = type.type
                System.arraycopy(data, 0, audioByteArray, 1, data.size)
                socket?.send(audioByteArray)
            }
            FrameDataType.Video -> {
                videoByteArray = ByteArray(data.size + 1)
                videoByteArray!![0] = type.type
                System.arraycopy(data, 0, videoByteArray!!, 1, data.size)
                socket?.send(videoByteArray)
                videoByteArray = null
            }
        }
    }

    fun dispose() {
        try {
            socket?.close()
            socketServer?.stop()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}