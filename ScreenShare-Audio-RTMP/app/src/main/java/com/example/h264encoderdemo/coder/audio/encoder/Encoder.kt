package com.example.h264encoderdemo.coder.audio.encoder

interface Encoder : Runnable {
    fun init()

    fun launch()

    fun dispose()
}