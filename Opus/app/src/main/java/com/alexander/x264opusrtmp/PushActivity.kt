package com.alexander.x264opusrtmp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.alexander.x264opusrtmp.databinding.ActivityPushBinding

class PushActivity : AppCompatActivity() {
    private val tag = "PushActivity"
    private lateinit var binding: ActivityPushBinding
    private var audioChannel: AudioChannel? = null
    private val livePusher by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LivePusher()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPushBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermission()
        audioChannel = AudioChannel(livePusher)
        audioChannel?.init("/data/data/com.alexander.x264opusrtmp/files/audio")
        audioChannel?.let {
            livePusher.audioChannel = it
        }
    }

    fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ), 1
            )
        }
        return false
    }

    fun startLive(view: View?) {
        livePusher.startLive("rtmp://192.168.0.104:1935/alex")
    }

    fun stopLive(view: View?) {
        livePusher.dispose()
    }
}