package com.example.h264playerdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import com.example.h264playerdemo.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var binding: ActivityMainBinding? = null
    private var h264Player: H264Player? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.surfaceView?.holder?.addCallback(this)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        val file = File(filesDir, "codec.h264")
        h264Player  = H264Player(p0.surface, file.absolutePath)
        h264Player?.play()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        h264Player?.dispose()
    }
}