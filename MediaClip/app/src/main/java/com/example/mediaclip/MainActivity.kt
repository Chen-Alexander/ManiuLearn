package com.example.mediaclip

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var videoView: VideoView? = null

    //    RangeSeekBar rangeSeekBar;
    var musicSeekBar: SeekBar? = null
    var videoSeekBar: SeekBar? = null
    var musicVolume = 0
    var videoVolume = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission(this)
        videoView = findViewById(R.id.videoView)
        musicSeekBar = findViewById(R.id.musicSeekBar)
        videoSeekBar = findViewById(R.id.videoSeekBar)
        musicSeekBar?.max = 100
        musicSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                musicVolume = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        videoSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                videoVolume = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    var runnable: Runnable? = null
    var duration = 0
    override fun onResume() {
        super.onResume()
        val aacPath = filesDir.absolutePath.plus("/music.mp3")
        val videoPath = filesDir.absolutePath.plus("/input.mp4")
        object : Thread() {
            override fun run() {
                try {
                    copyAssets("music.mp3", aacPath)
                    copyAssets("input.mp4", videoPath)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
        Thread.sleep(2000)
        startPlay(videoPath)
    }

    private fun startPlay(path: String) {
        val layoutParams = videoView!!.layoutParams
        layoutParams.height = 675
        layoutParams.width = 1285
        videoView!!.layoutParams = layoutParams
        videoView!!.setVideoPath(path)
        videoView!!.start()
        videoView!!.setOnPreparedListener { mp ->
            duration = mp.duration / 1000
            mp.isLooping = true
            val handler = Handler(Looper.getMainLooper())
            runnable = Runnable {
                handler.postDelayed(runnable!!, 1000)
            }
            handler.postDelayed(runnable!!, 1000)
        }
    }

    @Throws(IOException::class)
    private fun copyAssets(assetsName: String, path: String) {
        val assetFileDescriptor = assets.openFd(assetsName)
        val from = FileInputStream(assetFileDescriptor.fileDescriptor).channel
        val to = FileOutputStream(path).channel
        from.transferTo(assetFileDescriptor.startOffset, assetFileDescriptor.length, to)
    }

    fun music(view: View) {
//        大片制作的时候
//         剪辑的起始的时间  终止时间， 视频调整后的音乐大小 ，原生大小
        val filesDir = filesDir.absolutePath
        val videoFile = File(filesDir, "input.mp4")
        val audioFile = File(filesDir, "music.mp3")

//剪辑好的视频输出放哪里
        val outputFile = File(filesDir, "output.mp4")
        object : Thread() {
            override fun run() {
                try {
                    Log.e(TAG, "videoVolume:$videoVolume, musicVolume：$musicVolume")
                    MediaProcessor.mixAudioTrack(
                        this@MainActivity,
                        videoFile.absolutePath,
                        audioFile.absolutePath,
                        outputFile.absolutePath,
                        (60 * 1000 * 1000),
                        (100 * 1000 * 1000).toLong(),
                        videoVolume,
                        musicVolume
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    companion object {
        const val TAG = "MainActivity"

        fun checkPermission(
            activity: Activity
        ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activity.requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 1
                )
            }
            return false
        }
    }
}