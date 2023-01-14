package com.alexander.discovery

import android.net.nsd.NsdServiceInfo


interface IDiscoveryListener {
    fun onServiceRegisterSuccess(port: Int?)

    fun onServiceFound(info: NsdServiceInfo?)
}