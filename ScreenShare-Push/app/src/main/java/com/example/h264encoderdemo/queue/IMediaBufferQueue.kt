package com.example.h264encoderdemo.queue

interface IMediaBufferQueue {
    fun dequeue(size: Int): Int

    fun enqueue(index: Int)

    fun acquire(): Int

    fun release(index: Int)

    fun getBuf(index: Int): MediaBufferSlot

    fun dispose()
}