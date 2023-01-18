package com.example.h264encoderdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import com.example.h264encoderdemo.coder.video.decoder.H264Decoder
import com.example.h264encoderdemo.databinding.ActivityPlayerBinding
import com.example.h264encoderdemo.transmit.websocket.Receiver

class PlayerActivity : BaseActivity(), SurfaceHolder.Callback {

    companion object {
        private const val addressKey = "address"
        fun launch(caller: Context, address: String) {
            val intent = Intent(caller, PlayerActivity::class.java)
            intent.putExtra(addressKey, address)
            caller.startActivity(intent)
        }
    }

    private var binding: ActivityPlayerBinding? = null
//    private var player: H265Player? = null
    private var player: H264Decoder? = null
    private var receiver: Receiver? = null
    private var address: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        intent.getStringExtra(addressKey)?.let {
            address = it
        }
        binding?.surfaceView?.holder?.addCallback(this)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
//        player  = H265Player(p0.surface)
        player  = H264Decoder(p0.surface)
        player?.play()
        address?.let {
            receiver = Receiver(it)
        }
        receiver?.listener = player
        receiver?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        player?.dispose()
        receiver?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}