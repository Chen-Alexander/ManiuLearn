package com.example.cameraxdecoderpull

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import com.example.cameraxdecoderpull.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var binding: ActivityMainBinding? = null
//    private var player: H265Player? = null
    private var player: H264Player? = null
    private var socketLive: SocketLive? = null
    private lateinit var audioPlayerLive: AudioPlayerLive

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
        socketLive?.videoFrameListener = player
        audioPlayerLive = AudioPlayerLive(this)
        socketLive?.audioFrameListener = audioPlayerLive
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
        player?.dispose()
        socketLive?.dispose()
        audioPlayerLive.dispose()
    }
}