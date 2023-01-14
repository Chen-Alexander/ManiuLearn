package com.example.h264encoderdemo.queue

enum class MediaBufferState(val id: Short) {
    FREE(0),
    DEQUEUED(1),
    QUEUED(2),
    ACQUIRED(3);
}