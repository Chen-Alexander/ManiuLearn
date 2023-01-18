package com.example.h264encoderdemo.queue

import com.example.h264encoderdemo.queue.MediaBufferState.FREE

data class MediaBufferSlot(var buf: ByteArray, var state: MediaBufferState = FREE) {
    companion object {
        private fun createBuf(capacity: Int): ByteArray {
            return ByteArray(capacity)
        }

        fun create(capacity: Int): MediaBufferSlot {
            return MediaBufferSlot(buf = createBuf(capacity))
        }

        fun create(): MediaBufferSlot {
            return MediaBufferSlot(buf = ByteArray(0))
        }
    }

    fun capacity(): Int {
        return buf.size
    }

    fun resize(size: Int) {
        buf = createBuf(size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaBufferSlot

        if (!buf.contentEquals(other.buf)) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = buf.contentHashCode()
        result = 31 * result + state.hashCode()
        return result
    }
}
