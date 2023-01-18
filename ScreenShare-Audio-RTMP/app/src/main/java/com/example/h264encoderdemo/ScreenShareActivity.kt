package com.example.h264encoderdemo

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import com.example.h264encoderdemo.beans.RTMPPacket
import com.example.h264encoderdemo.coder.audio.encoder.AudioEncoder
import com.example.h264encoderdemo.coder.video.encoder.H264Encoder
import com.example.h264encoderdemo.coder.video.encoder.VideoEncoder
import com.example.h264encoderdemo.databinding.ActivityScreenShareBinding
import com.example.h264encoderdemo.transmit.rtmp.RTMPSender
import com.example.h264encoderdemo.transmit.websocket.WebSocketSender
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class ScreenShareActivity : BaseActivity() {
    private val tag = "ScreenShareActivity"
    private var binding: ActivityScreenShareBinding? = null
    private var mediaProjectionMgr: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var videoEncoder: VideoEncoder? = null
    private var audioEncoder: AudioEncoder? = null
    private val requestCode = 1
    private var rtmpSender: RTMPSender? = null
    private val size = Point(0, 0)
    private var densityPercent by Delegates.notNull<Float>()
    // 音视频缓存队列，生产消费模式，VideoEncoder/AudioEncoder往队列offer数据，sender从队列内take数据
    private var queue = LinkedBlockingQueue<RTMPPacket>(64)
    private var defaultFPS = 20
    private var defaultGOPSize = 15
    private val screenShareResult = registerForActivityResult(ScreenShare()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            mediaProjection = mediaProjectionMgr?.getMediaProjection(result.resultCode, result.data!!)
            // surface: the input of mediaCode-Encoder, the output of mediaProjection
            videoEncoder?.createInputSurface()?.let { inputSurface ->
                mediaProjection?.createVirtualDisplay(tag, size.x, size.y, densityPercent.roundToInt(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, inputSurface, null,
                    null)
            }
            rtmpSender?.launch()
            videoEncoder?.launch()
            audioEncoder?.launch()
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
        videoEncoder = H264Encoder(size.x, size.y, defaultFPS, defaultGOPSize, queue)
        videoEncoder?.init()
        audioEncoder = AudioEncoder(queue)
        audioEncoder?.init()
        // 获取上个页面传递过来的本地地址
        intent?.getStringExtra("rtmp-url")?.let {
            if (!isEmpty(it)) {
                rtmpSender = RTMPSender(it, queue)
                makeText(this, "推流地址为空", LENGTH_SHORT).show()
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
        videoEncoder?.dispose()
        videoEncoder = null
        audioEncoder?.dispose()
        audioEncoder = null
        rtmpSender?.dispose()
        rtmpSender = null
        queue.clear()
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
