package com.example.h264encoderdemo.queue

import com.example.h264encoderdemo.queue.MediaBufferState.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @param perBufSize The size of each buf
 * */
class MediaBufferQueue() : IMediaBufferQueue {

    private val defaultCapacity = 16
    // 所有数据都在slots中存储
    private lateinit var bufSlots: MutableList<MediaBufferSlot>
    // 队列控制生产消费的缓冲
    private lateinit var queue: LinkedBlockingQueue<Int>

    init {
        initQueue(defaultCapacity)
    }

    constructor(capacity: Int): this() {
        initQueue(capacity)
    }

    private fun initQueue(capacity: Int) {
        // slot集合最少21个：16 / 0.75
        val localCapacity = max(defaultCapacity, capacity)
        val slotSize = localCapacity / 0.75
        // default size is 1
        bufSlots = MutableList(slotSize.roundToInt()) { MediaBufferSlot.create() }
        queue = LinkedBlockingQueue(capacity)
    }

    override fun dequeue(size: Int): Int {
        var result = -1
        synchronized(bufSlots) {
            val frees = bufSlots.filter { it.state == FREE }
            if (frees.isNotEmpty()) {
                // 有free的slot，寻找最合适的: 等于、大于size但是最接近size
                var target: MediaBufferSlot? = null
                val candidate = frees[0].capacity()
                var minDif = Int.MAX_VALUE
                bufSlots.forEachIndexed { index, value ->
                    if (value.state == FREE && value.capacity() == size) {
                        // 如果正好有size相等的slot，则直接使用即可
                        value.state = DEQUEUED
                        return index
                    } else if (value.state == FREE && value.capacity() > size) {
                        // 从capacity > size的slot中找出一个最接近size的
                        val dif = abs(candidate - value.capacity())
                        if (dif <= minDif) {
                            minDif = dif
                            target = value
                            result = index
                        }
                    }
                }
                // 判断是否找到了最接近size的slot
                target?.let {
                    // 空间相差不能大于8倍
                    // todo 添加策略，当queue内缓存的数据达到一定水平后，如果多次命中相差八倍的slot，则resize当前被命中的slot
                    if (it.capacity() / size <= 8) {
                        it.state = DEQUEUED
                        return result
                    }
                }
                // 经遍历，没有大于等于size的slot，则从集合中找出一个free的slot，并resize其空间
                bufSlots.forEachIndexed { index, value ->
                    if (value.state == FREE) {
                        value.state = DEQUEUED
                        requestBuf(size, value)
                        result = index
                        return result
                    }
                }
            } else {
                // 无free的slot，从队头移除一个(此时队列肯定是满的，所以poll肯定可以拿到数据)
                val index = queue.take()
                val slot = bufSlots[index]
                if (slot.capacity() < size) {
                    requestBuf(size, slot)
                }
                // 状态直接改为free
                slot.state = FREE
                return index
            }
        }
        return result
    }

    private fun requestBuf(size: Int, slot: MediaBufferSlot) {
        if (slot.capacity() == size) {
            return
        }
        slot.resize(size)
    }

    override fun enqueue(index: Int) {
        synchronized(bufSlots) {
            val slot = bufSlots[index]
            slot.state = QUEUED
        }
        queue.offer(index)
    }

    override fun acquire(): Int {
        val index = queue.take()
        synchronized(bufSlots) {
            val slot = bufSlots[index]
            slot.state = ACQUIRED
        }
        return index
    }

    override fun release(index: Int) {
        synchronized(bufSlots) {
            val slot = bufSlots[index]
            slot.state = FREE
        }
    }

    override fun getBuf(index: Int): MediaBufferSlot {
        return bufSlots[index]
    }

    override fun dispose() {
        bufSlots.clear()
        queue.clear()
    }
}