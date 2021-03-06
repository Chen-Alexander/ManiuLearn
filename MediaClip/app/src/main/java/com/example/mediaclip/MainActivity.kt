package com.example.mediaclip

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
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
    var voiceSeekBar: SeekBar? = null
    var musicVolume = 0
    var voiceVolume = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission(this)
        videoView = findViewById(R.id.videoView)
        //        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        musicSeekBar = findViewById(R.id.musicSeekBar)
        voiceSeekBar = findViewById(R.id.voiceSeekBar)
        musicSeekBar?.max = 100
        musicSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                musicVolume = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        voiceSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                voiceVolume = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    var runnable: Runnable? = null
    var duration = 0
    override fun onResume() {
        super.onResume()
        object : Thread() {
            override fun run() {
                val aacPath =
                    File(Environment.getExternalStorageDirectory(), "music.mp3").absolutePath
                val videoPath =
                    File(Environment.getExternalStorageDirectory(), "input.mp4").absolutePath
                try {
                    copyAssets("music.mp3", aacPath)
                    copyAssets("input.mp4", videoPath)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
        startPlay(File(Environment.getExternalStorageDirectory(), "input.mp4").absolutePath)
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
            //                rangeSeekBar.setRange(0, duration);
            //                rangeSeekBar.setValue(0, duration);
            //                rangeSeekBar.setEnabled(true);
            //                rangeSeekBar.requestLayout();
            //                rangeSeekBar.setOnRangeChangedListener(new RangeSeekBar.OnRangeChangedListener() {
            //                    @Override
            //                    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
            //                        videoView.seekTo((int) min * 1000);
            //                    }
            //                });
            val handler = Handler()
            runnable =
                Runnable { //                        if (videoView.getCurrentPosition() >= rangeSeekBar.getCurrentRange()[1] * 1000) {
                    //                            videoView.seekTo((int) rangeSeekBar.getCurrentRange()[0] * 1000);
                    //                        }
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
//        ?????????????????????
//         ????????????????????????  ??????????????? ?????????????????????????????? ???????????????
        val cacheDir = Environment.getExternalStorageDirectory()
        val videoFile = File(cacheDir, "input.mp4")
        val audioFile = File(cacheDir, "music.mp3")

//?????????????????????????????????
        val outputFile = File(cacheDir, "output.mp4")
        object : Thread() {
            override fun run() {
                try {
//                    MusicProcess.mixAudioTrack(
//                        videoFile.absolutePath,
//                        audioFile.absolutePath,
//                        outputFile.absolutePath,
//                        (60 *
//                                1000 * 1000),
//                        (100 * 1000 * 1000) as Int,
//                        voiceVolume,
//                        musicVolume
//                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    companion object {
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