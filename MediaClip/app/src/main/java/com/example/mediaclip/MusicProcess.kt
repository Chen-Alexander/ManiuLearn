package com.example.mediaclip

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


object MusicProcess {
    private const val timeout = 1_000L
    private const val tag = "MusicProcess"

    public fun mixAudioTrack(
        context: Context, videoInput: String, audioInput: String, output: String,
        timeTimeUs: Int, endTimeUs: Int, videoVolume: Int, audioVolume: Int
    ) {
        val fileDir = context.filesDir
        // 视频的原始PCM
        val videoPcmFile = File(fileDir, "video.pcm")

        // 音频的原始PCM
        val audioPcmFile = File(fileDir, "audio.pcm")
    }

    /**
     * 从多媒体文件中截取音频并转化为PCM格式
     * @param mediaPath: 多媒体文件路径
     * @param output pcm的输出路径
     * @param startTime 时间起点
     * @param endTime 时间终点*/
    private fun decodePCM(
        mediaPath: String, output: String, startTime: Long, endTime: Long) {
        if (endTime <= startTime) {
             Log.e(tag, "time error!")
            return
        }
        var mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(mediaPath)
        // 拿到音频索引
        val audioTrack = findTrack(mediaExtractor)
        // 选择需要处理的轨道
        mediaExtractor.selectTrack(audioTrack)
        // seek到起始时间点后的一个关键帧处
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_NEXT_SYNC)
        val audioFormat = mediaExtractor.getTrackFormat(audioTrack)
        val mediaCodec = audioFormat.getString(MediaFormat.KEY_MIME)?.let { MediaCodec.createDecoderByType(it) }
        mediaCodec?.configure(audioFormat, null, null, 0)
        mediaCodec?.start()
        // 如果包含最大输入空间的数据则取值
        val maxBufSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            100 * 1024
        }
        // 准备pcm文件写入
        val pcmFile = File(output)
        val fileChannel = FileOutputStream(pcmFile).channel
        // 申请buffer,用以缓存每一帧数据
        val buf = ByteBuffer.allocateDirect(maxBufSize)
        val bufInfo = MediaCodec.BufferInfo()
        while (true) {
            // 获取dsp的输入缓冲区
            val inIndex = mediaCodec?.dequeueInputBuffer(timeout)
            if (inIndex != null && inIndex >= 0) {
                // 判断当前帧是否在时间区间内
                val curSampleTime = mediaExtractor.sampleTime
                if (curSampleTime < startTime) {
                    // 不在时间区间内，丢弃
                    mediaExtractor.advance()
                } else if (curSampleTime > endTime) {
                    // 超出时间区间
                    break
                } else if (curSampleTime == -1L) {
                    // 文件已经读取完，但是时间还没有达到endTime
                    break
                }

                // 从mediaExtractor中读取数据，准备需要解码的数据(此时是aac数据)
                bufInfo.size = mediaExtractor.readSampleData(buf, 0)
                bufInfo.presentationTimeUs = curSampleTime
                @SuppressLint("WrongConstant")
                bufInfo.flags = mediaExtractor.sampleFlags
//                val content = ByteArray(buf.remaining())
//                buf.get(content)
                val inputBuf = mediaCodec.getInputBuffer(inIndex)
                inputBuf?.put(buf)
                mediaCodec.queueInputBuffer(inIndex, 0, bufInfo.size,
                    bufInfo.presentationTimeUs, bufInfo.flags)
                // 释放上一帧数据，否则不会走到下一帧
                mediaExtractor.advance()
            }
        }
    }

    /**
     * @param audio 是否寻找音频轨*/
    private fun findTrack(extractor: MediaExtractor, audio: Boolean = true): Int {
        val trackCount = extractor.trackCount
        for (index in 0..trackCount) {
            // 获取当前媒体封装文件的轨道配置信息
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (audio) {
                if (mime?.startsWith("audio") == true) {
                    return index
                }
            } else {
                if (mime?.startsWith("video") == true) {
                    return index
                }
            }
        }
        return -1
    }
}