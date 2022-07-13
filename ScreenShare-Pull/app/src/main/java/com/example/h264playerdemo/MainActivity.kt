package com.example.h264playerdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import com.example.h264playerdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var binding: ActivityMainBinding? = null
//    private var player: H265Player? = null
    private var player: H264Player? = null
    private var socketLive: SocketLive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.surfaceView?.holder?.addCallback(this)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
//        player  = H265Player(p0.surface)
        player  = H264Player(p0.surface)
        player?.play()
        socketLive = SocketLive()
        socketLive?.listener = player
        socketLive?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        player?.dispose()
        socketLive?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}