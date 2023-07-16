package com.alexander.x264opusrtmp

import android.media.AudioFormat.*

object Constants {
    const val channelCount = 1
    val inChannelConfig = if (channelCount == 2) CHANNEL_IN_STEREO else CHANNEL_IN_MONO
    val outChannelConfig = if (channelCount == 2) CHANNEL_OUT_STEREO else CHANNEL_OUT_MONO
    const val sampleRate = 48000
//    const val sampleRate = 16000
    const val encodeBitRate = 16000
}