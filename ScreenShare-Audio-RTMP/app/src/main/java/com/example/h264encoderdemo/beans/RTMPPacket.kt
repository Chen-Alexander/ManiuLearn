package com.example.h264encoderdemo.beans

data class RTMPPacket(
    var type: Int,
    var tms: Long,
) {
    var buffer: ByteArray? = null
}

enum class RTMPPacketType(val type: Int) {
    RTMP_PACKET_TYPE_AUDIO_HEAD(0),
    RTMP_PACKET_TYPE_AUDIO_DATA(1),
    RTMP_PACKET_TYPE_VIDEO(2)
}