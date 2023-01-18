package com.example.h264encoderdemo.transmit.websocket

import android.util.Log
import com.example.h264encoderdemo.queue.MediaBufferQueue
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress

class WebSocketSender(private val queue: MediaBufferQueue, private val port: Int): Runnable {
    private val tag = "SocketLive"
    private var socket: WebSocket? = null
    private var socketServer: WebSocketServer? = null
    private var sendThread: Thread? = null
    @Volatile
    private var push = false

    init {
        socketServer = object : WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Log.i(tag, "socketServer onOpen")
                this@WebSocketSender.socket = conn
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
        sendThread = Thread(this, "Sender-Thread")
    }

    fun launch() {
        socketServer?.start()
        sendThread?.start()
    }

    fun enable(enabled: Boolean) {
        push = enabled
    }

    override fun run() {
        while (true) {
            runCatching {
                if (socket != null && socket!!.isOpen) {
                    val pair = acquireBuf()
                    socket?.send(pair.second)
                    releaseBuf(pair.first)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private fun acquireBuf(): Pair<Int, ByteArray> {
        // 没有数据时会产生阻塞，所以不用判断index > -1
        val index = queue.acquire()
        val buf = queue.getBuf(index).buf
        return Pair(index, buf)
    }

    private fun releaseBuf(index: Int) {
        queue.release(index)
    }

    fun dispose() {
        try {
            socket?.close()
            socket = null
            socketServer?.stop()
            socketServer = null
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}