package com.alexander.x264opusrtmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alexander.x264opusrtmp.databinding.ActivityMainBinding
import com.alexander.x264opusrtmp.util.PcmToWavUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()

        binding.button.setOnClickListener {
            val intent = Intent(this, PushActivity::class.java)
            startActivity(intent)
        }

        binding.playOpus.setOnClickListener {
            val livePusher = LivePusher()
            val audioPlayer = AudioPlayer(this, livePusher)
            audioPlayer.init("/data/data/com.alexander.x264opusrtmp/files/audio.opus")
            audioPlayer.launch()
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
}