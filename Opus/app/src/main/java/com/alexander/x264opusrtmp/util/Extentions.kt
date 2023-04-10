package com.alexander.x264opusrtmp.util

import java.nio.ByteBuffer

fun ByteBuffer.toByteArrayBigEndian(): ByteArray {
    val byteArray = ByteArray(this.remaining())
    for (i in byteArray.indices) {
        byteArray[i] = this.get(i)
    }
    byteArray.reverse()
    return byteArray
}

fun ByteArray.toByteArrayBigEndian(): ByteArray {
    val byteArray = this.copyOf()
    byteArray.reverse()
    return byteArray
}
