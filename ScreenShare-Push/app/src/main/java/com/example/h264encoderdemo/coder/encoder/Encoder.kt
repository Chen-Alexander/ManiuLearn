package com.example.h264encoderdemo.coder.encoder

import android.view.Surface

interface Encoder : Runnable {
    fun init()

    fun createInputSurface(): Surface?

    fun launch()

    fun dispose()
}