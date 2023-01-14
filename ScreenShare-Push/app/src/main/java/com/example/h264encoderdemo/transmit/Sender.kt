package com.example.h264encoderdemo.transmit

import android.util.Log
import com.example.h264encoderdemo.coder.encoder.H264Encoder
import com.example.h264encoderdemo.coder.encoder.ScreenShareDataListener
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress

class Sender(private val encoder: H264Encoder) : ScreenShareDataListener {
    private val tag = "SocketLive"
    private var socket: WebSocket? = null
    private var socketServer: WebSocketServer? = null

    init {
        encoder.dataListener = this
        socketServer = object : WebSocketServer(InetSocketAddress(59880)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Log.i(tag, "socketServer onOpen")
                this@Sender.socket = conn
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
        encoder.launch()
    }

    override fun onFrame(data: ByteArray) {
        if (socket?.isOpen == true) {
            socket?.send(data)
        }
    }

    fun dispose() {
        try {
            encoder.dispose()
            socket?.close()
            socketServer?.stop()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}