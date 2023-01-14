package com.example.h264encoderdemo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.io.IOException
import java.net.ServerSocket


object NetUtil {
    @Throws(IOException::class)
    fun getLocalIP(context: Context): String? {
        val info = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        if (info.type == ConnectivityManager.TYPE_WIFI) {
            val wifiMg = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiMg.connectionInfo
            val ip = intIP2StringIP(wifiInfo.ipAddress)
            return ip
        }
        return null
    }

    private fun intIP2StringIP(ip: Int): String? {
        return (ip and 0xFF).toString() + "." +
                (ip shr 8 and 0xFF) + "." +
                (ip shr 16 and 0xFF) + "." +
                (ip shr 24 and 0xFF)
    }

    @Throws(IOException::class)
    fun getPort(): Int {
        //读取空闲的可用端口
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort
        serverSocket.close()
        return port
    }
}