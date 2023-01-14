package com.example.h264encoderdemo

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import com.example.h264encoderdemo.coder.encoder.H264Encoder
import com.example.h264encoderdemo.coder.encoder.VideoEncoder
import com.example.h264encoderdemo.databinding.ActivityScreenShareBinding
import com.example.h264encoderdemo.queue.MediaBufferQueue
import com.example.h264encoderdemo.transmit.Sender
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class ScreenShareActivity : BaseActivity() {
    private val tag = "ScreenShareActivity"
    private var binding: ActivityScreenShareBinding? = null
    private var mediaProjectionMgr: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var encoder: VideoEncoder? = null
    private val requestCode = 1
    private var sender: Sender? = null
    private val size = Point(0, 0)
    private var densityPercent by Delegates.notNull<Float>()
    private var queue = MediaBufferQueue()
    private var defaultFPS = 20
    private var defaultGOPSize = 15
    private val screenShareResult = registerForActivityResult(ScreenShare()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            mediaProjection = mediaProjectionMgr?.getMediaProjection(result.resultCode, result.data!!)
            // surface: the input of mediaCode-Encoder, the output of mediaProjection
            encoder?.createInputSurface()?.let { inputSurface ->
                mediaProjection?.createVirtualDisplay(tag, size.x, size.y, densityPercent.roundToInt(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, inputSurface, null,
                    null)
            }
            sender?.launch()
            encoder?.launch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenShareBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        // 获取屏幕的宽高，作为MediaFormat中的宽高
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        densityPercent = displayMetrics.density
        windowManager.defaultDisplay.getSize(size)
        encoder = H264Encoder(size.x, size.y, defaultFPS, defaultGOPSize, queue)
        encoder?.init()
        // 获取上个页面传递过来的本地地址
        intent?.getIntExtra("listeningPort", -1)?.let {
            if (it != -1) {
                sender = Sender(queue, it)
            } else {
                makeText(this, "本地监听端口为空", LENGTH_SHORT).show()
            }
        }

        mediaProjectionMgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?
        val intent = mediaProjectionMgr?.createScreenCaptureIntent()
        screenShareResult.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy() called")
        mediaProjectionMgr = null
        mediaProjection?.stop()
        mediaProjection = null
        encoder?.dispose()
        encoder = null
        sender?.dispose()
        sender = null
        queue.dispose()
    }
}

internal class ScreenShare : ActivityResultContract<Intent, ActivityResult>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode, intent)
    }
}
