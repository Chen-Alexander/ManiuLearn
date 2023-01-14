package com.example.h264encoderdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.BaseAdapter
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.alexander.discovery.DiscoveryMgr
import com.alexander.discovery.DiscoveryMgr.Companion.addressKey
import com.alexander.discovery.IDiscoveryListener
import com.example.h264encoderdemo.databinding.ActivityMainBinding

class MainActivity : BaseActivity(), IDiscoveryListener {
    private val tag = "MainActivity"

    private var binding: ActivityMainBinding? = null
    private val remoteAddressList: MutableList<Map<String, String>> = mutableListOf()
    private val defaultKey = "name"
    private val startActivityForResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            Log.d(tag, "onActivityResult() called with: result = $result")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        DiscoveryMgr.instance().init(this)
        // 注册，并记录本地地址
        binding?.register?.setOnClickListener {
            DiscoveryMgr.instance().exposure(this)
        }
        // 发现，获取、保存、展现远端地址
        binding?.find?.setOnClickListener {
            DiscoveryMgr.instance().find()
        }
        binding?.share?.setOnClickListener {
        }
        val adapter = SimpleAdapter(
            this,
            remoteAddressList,
            android.R.layout.simple_list_item_1,
            arrayOf(defaultKey),
            intArrayOf(android.R.id.text1)
        )
        binding?.listView?.adapter = adapter
        binding?.listView?.setOnItemClickListener { _, _, position, _ ->
            remoteAddressList[position][defaultKey]?.let {
                PlayerActivity.launch(this, it)
            }
        }

        checkPermission()
    }

    override fun onServiceRegisterSuccess(port: Int?) {
        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
        runOnUiThread {
            val intent = Intent(this@MainActivity, ScreenShareActivity::class.java)
            intent.putExtra("listeningPort", port)
            startActivity(intent)
        }
    }

    override fun onServiceFound(info: NsdServiceInfo?) {
        info?.attributes?.get(addressKey)?.let { bytes ->
            val address = String(bytes)
            Log.i(tag, "onServiceFound:${info.serviceName}, 地址:$address")
            val map = mutableMapOf(defaultKey to address)
            if (!remoteAddressList.contains(map)) {
                remoteAddressList.add(map)
            }
            runOnUiThread {
                (binding?.listView?.adapter as? BaseAdapter)?.notifyDataSetChanged()
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
                ), 1
            )
        }
        return false
    }
}