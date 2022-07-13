package com.example.h264encoderdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import com.example.h264encoderdemo.databinding.ActivityMainBinding
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
//    private var h265Encoder: H265Encoder? = null
    private val requestCode = 1
    private var socketLive: SocketLive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        checkPermission()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
        binding?.button?.setOnClickListener {
            val intent = mediaProjectionManager?.createScreenCaptureIntent()
            startActivityForResult(intent, requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == this.requestCode && data != null) {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val densityPercent = displayMetrics.density
            val width = windowManager.defaultDisplay.width
            val height = windowManager.defaultDisplay.height
            mediaProjection?.let {
//                val encoder = H265Encoder(it, width, height, densityPercent.roundToInt())
                val encoder = H264Encoder(it, width, height, densityPercent.roundToInt())
                encoder.init()
                socketLive = SocketLive(encoder)
                socketLive?.start()
//                h265Encoder?.launch()
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), 1
            )
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
//        h265Encoder?.dispose()
//        h265Encoder = null
        socketLive?.dispose()
        socketLive = null
    }
}
