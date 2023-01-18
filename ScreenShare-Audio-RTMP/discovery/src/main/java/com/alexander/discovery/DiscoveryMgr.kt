package com.alexander.discovery

import android.content.Context
import android.content.Context.NSD_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.PROTOCOL_DNS_SD
import android.net.nsd.NsdManager.ResolveListener
import android.net.nsd.NsdServiceInfo
import android.util.ArrayMap
import android.util.Log
import com.alexander.discovery.util.NetUtil

class DiscoveryMgr : IDiscovery {
    private val tag = "DiscoveryMgr"
    private var nsdManager: NsdManager? = null
    private val serviceName = "探路者"
    private var serviceType = "_discovery._tcp"
    private var registerListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    var serviceFoundListener: IDiscoveryListener? = null

    override fun init(context: Context) {
        nsdManager = context.getSystemService(NSD_SERVICE) as? NsdManager
        if (context is IDiscoveryListener) {
            serviceFoundListener = context
        }
    }

    override fun exposure(context: Context): Int {
        val localPort = NetUtil.getPort()
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = this@DiscoveryMgr.serviceName
            this.serviceType = this@DiscoveryMgr.serviceType
            this.port = localPort
            NetUtil.getLocalIP(context)?.let {
                this.setAttribute(addressKey, it.plus(':').plus(this.port))
            }
        }
        registerListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.i(tag, "onServiceRegistered:${serviceInfo?.serviceName}")
                serviceFoundListener?.onServiceRegisterSuccess(localPort)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.i(tag, "onRegistrationFailed:${serviceInfo?.serviceName}, err:$errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Log.i(tag, "onServiceUnregistered:${serviceInfo?.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.i(tag, "onUnregistrationFailed:${serviceInfo?.serviceName}, err:$errorCode")
            }
        }
        nsdManager?.registerService(serviceInfo, PROTOCOL_DNS_SD, registerListener)
        return serviceInfo.port
    }

    override fun stopExposure() {
        registerListener?.let {
            nsdManager?.unregisterService(it)
        }
    }

    override fun find() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "onStartDiscoveryFailed:$serviceType, err:$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(tag, "onStopDiscoveryFailed:$serviceType, err:$errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(tag, "onDiscoveryStarted:$serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(tag, "onDiscoveryStopped:$serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.i(tag, "onServiceFound:${serviceInfo?.serviceName}")
                nsdManager?.resolveService(serviceInfo, object : ResolveListener {
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        serviceFoundListener?.onServiceFound(serviceInfo)
                    }

                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        Log.w(tag, "onResolveFailed:${serviceInfo?.serviceName}")
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.w(tag, "onServiceLost:${serviceInfo?.serviceName}")
            }
        }
        nsdManager?.discoverServices(serviceType, PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stopFind() {
        discoveryListener?.let {
            nsdManager?.stopServiceDiscovery(it)
        }
    }

    override fun dispose() {
        registerListener?.let {
            nsdManager?.unregisterService(it)
        }
        discoveryListener?.let {
            nsdManager?.stopServiceDiscovery(it)
        }
        nsdManager = null
    }

    companion object {
        const val addressKey = "address"
        @Volatile
        private var discoveryMgr: DiscoveryMgr? = null

        fun instance(): DiscoveryMgr {
            if (discoveryMgr == null) {
                synchronized(DiscoveryMgr::class.java) {
                    if (discoveryMgr == null) {
                        discoveryMgr = DiscoveryMgr()
                        return discoveryMgr!!
                    }
                }
            }
            return discoveryMgr!!
        }
    }
}