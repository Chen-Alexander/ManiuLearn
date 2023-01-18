package com.alexander.discovery

import android.content.Context

interface IDiscovery {
    fun init(context: Context)

    /**
     * 暴露服务
     * */
    fun exposure(context: Context): Int

    fun stopExposure()

    /**
     * 发现服务
     * */
    fun find()

    fun stopFind()

    fun dispose()
}