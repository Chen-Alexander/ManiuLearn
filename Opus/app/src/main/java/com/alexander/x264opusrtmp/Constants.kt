package com.alexander.x264opusrtmp

import android.media.AudioFormat.*

object Constants {
    const val channelCount = 1
    val inChannelConfig = if (channelCount == 2) CHANNEL_IN_STEREO else CHANNEL_IN_MONO
    val outChannelConfig = if (channelCount == 2) CHANNEL_OUT_STEREO else CHANNEL_OUT_MONO
    const val aacSampleRate = 48000
    const val opusSampleRate = 16000
    const val opusEncodeBitRate = 48000
}