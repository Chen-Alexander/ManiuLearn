package com.example.cameraxencoderpush

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraxencoderpush.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)
    private var y: ByteArray? = null
    private var u: ByteArray? = null
    private var v: ByteArray? = null
    private var firstResult: ByteArray? = null
    private var secondResult: ByteArray? = null
    private lateinit var h264Encoder: H264Encoder
    private lateinit var audioRecorderLive: AudioRecorderLive
    private val socketLive = SocketLive()

    private var count = 0

    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        socketLive.start()
        if (!::audioRecorderLive.isInitialized) {
            audioRecorderLive = AudioRecorderLive(this)
            audioRecorderLive.frameDataListener = socketLive
        }
        binding.connect.setOnClickListener {
            // 开始capture和预览，并编码发送视频帧
            bindCameraUseCases()
            // 开始录音并发送pcm音频数据
            audioRecorderLive.startRecord()
        }
    }

    private fun bindCameraUseCases() = binding.previewView.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector  =CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val preview = Preview.Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .setTargetRotation(binding.previewView.display?.rotation!!)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//                ImgHelper.useYuvImgSaveFile(image, false)

                if (firstResult == null) {
                    val yuvSize = (image.width * image.height * 1.5F).toInt()
                    firstResult = ByteArray(yuvSize)
                }
                if (secondResult == null) {
                    val yuvSize = (image.width * image.height * 1.5F).toInt()
                    secondResult = ByteArray(yuvSize)
                }
                ImgHelper.toYuvImageNV21(image, firstResult!!)
                YuvUtils.NV21ToI420(firstResult, secondResult, image.width, image.height)
                YuvUtils.Flip(secondResult, firstResult, image.width, image.height)
                YuvUtils.RotateI420(firstResult, secondResult, image.width, image.height, 90)
//                count++
//                if (count == 30) {
//                    // 保存第30帧的YUV数据（此处保存的yuv数据用ffplay播放是很正常的）
//                    FileUtils.writeBytes(secondResult!!)
//                }

                if (!::h264Encoder.isInitialized) {
                    h264Encoder = H264Encoder(image.height, image.width)
                    h264Encoder.init()
                    h264Encoder.frameDataListener = socketLive
                }
                image.close()
                h264Encoder.encodeFrame(secondResult!!)

                computeFPS()
            })

            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun computeFPS() {
        // Compute the FPS of the entire pipeline
        val frameCount = 10
        if (++frameCounter % frameCount == 0) {
            frameCounter = 0
            val now = System.currentTimeMillis()
            val delta = now - lastFpsTimestamp
            val fps = 1000 * frameCount.toFloat() / delta
            Log.d(tag, "FPS: ${"%.02f".format(fps)}")
            lastFpsTimestamp = now
        }
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        } else {
            binding.connect.isEnabled = true
        }
    }

    override fun onDestroy() {
        // Terminate all outstanding analyzing jobs (if there is any).
        executor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
        h264Encoder.dispose()
        socketLive.dispose()
        audioRecorderLive.dispose()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            binding.connect.isEnabled = true
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}