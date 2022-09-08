package com.example.mediaclip

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.media.AudioFormat.CHANNEL_IN_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC
import android.media.MediaExtractor.SEEK_TO_NEXT_SYNC
import android.media.MediaFormat.KEY_MAX_INPUT_SIZE
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or


object MusicProcess {
    private const val timeout = 1_000L
    private const val tag = "MusicProcess"
    private const val defaultSampleRate = 44100
    private const val defaultChannelCount = 2

    fun mixAudioTrack(
        context: Context, videoInput: String, audioInput: String, output: String,
        startTimeUs: Long, endTimeUs: Long, videoVolume: Int, audioVolume: Int
    ) {
        val fileDir = context.filesDir
        // 视频的原始PCM
        val videoPcmFile = File(fileDir, "video.pcm")
        decodePCM(videoInput, videoPcmFile.absolutePath, startTimeUs, endTimeUs)
        // 新音频的PCM
        val audioPcmFile = File(fileDir, "audio.pcm")
        decodePCM(audioInput, audioPcmFile.absolutePath, startTimeUs, endTimeUs)
        // 混音
        val mixedPcm = File(fileDir, "mixed.pcm")
//        mixPcm(videoPcmFile.absolutePath, audioPcmFile.absolutePath, videoVolume, audioVolume,
//            mixedPcm.absolutePath)
        TestUtil.mixPcm(videoPcmFile.absolutePath, audioPcmFile.absolutePath, mixedPcm.absolutePath,
            videoVolume, audioVolume)

        val wavFile = File(fileDir, mixedPcm.name.plus(".wav"))
        PcmToWavUtil(44100, CHANNEL_IN_STEREO, 2, ENCODING_PCM_16BIT)
            .pcmToWav(mixedPcm.absolutePath, wavFile.absolutePath)
        // 把原视频和新音频打包写入新文件中
        mixVideoAndAudio(videoInput, output, startTimeUs, endTimeUs, wavFile.absolutePath)
    }

    @SuppressLint("WrongConstant")
    private fun mixVideoAndAudio(videoInput: String, output: String, startTime: Long, endTime: Long,
                                 wavFilePath: String) {
        // 媒体复用器，用于打包编码后的视频帧和音频帧并输出到文件中
        val mediaMuxer = MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // 媒体解复用器，用于从源mp4文件中提取出未解码的视频帧和音频帧
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(videoInput)
        // 从源mp4文件中分别找到音频和视频的轨道索引
        val audioTrackIndex = findTrack(mediaExtractor)
        val videoTrackIndex = findTrack(mediaExtractor, MediaType.Video)

        // 获取源mp4文件的视频格式属性,因为只是混音，所以这里可以复用源videoFormat
        val videoFormat = mediaExtractor.getTrackFormat(videoTrackIndex)
        // 复用器添加一个视频轨
        mediaMuxer.addTrack(videoFormat)
        // 获取源mp4文件的音频格式属性,
        val audioFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
        // 记录源音频的比特率
        val srcAudioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        // 设置混音后的新音频的编码格式
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        // 复用器添加一个音频轨（记录新文件中的音频轨的索引）
        val muxerAudioIndex = mediaMuxer.addTrack(audioFormat)
        // 启动复用器
        mediaMuxer.start()
        // 先处理音频
        mediaExtractor.selectTrack(audioTrackIndex)

        // 解析新音频的解复用器
        val pcmExtractor = MediaExtractor()
        pcmExtractor.setDataSource(wavFilePath)
        val pcmAudioTrackIndex = findTrack(pcmExtractor)
        pcmExtractor.selectTrack(pcmAudioTrackIndex)
        // 新音频的音频格式属性
        val pcmAudioFormat = pcmExtractor.getTrackFormat(pcmAudioTrackIndex)
        // 尝试获取新音频的最大一帧的大小
        val maxBufSizeOfPcm = if (pcmAudioFormat.containsKey(KEY_MAX_INPUT_SIZE)) {
            pcmAudioFormat.getInteger(KEY_MAX_INPUT_SIZE)
        } else {
            100 * 1024
        }

        // 开始编码新音频并写入到文件
        // 实例化音频编码器并设置其需要的相关数据
        val newAudioEncodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
            defaultSampleRate, defaultChannelCount)
        // 设置比特率，沿用原视频文件中的音频比特率
        newAudioEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, srcAudioBitrate)
        // 设置音质等级
        newAudioEncodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AACObjectLC)
        // 设置音频最大帧的大小
        newAudioEncodeFormat.setInteger(KEY_MAX_INPUT_SIZE, maxBufSizeOfPcm)
        val newAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        newAudioEncoder.configure(newAudioEncodeFormat, null, null, CONFIGURE_FLAG_ENCODE)
        newAudioEncoder.start()
        // 申请内存用以盛放读取到的pcm数据
        var buf = ByteBuffer.allocateDirect(maxBufSizeOfPcm)
        val bufInfo = MediaCodec.BufferInfo()
        // 标记音频编码是否完成
        var audioEncodeDone = false
        while (!audioEncodeDone) {
            // 开始获取并使用dsp编码一帧音频
            val inputBufIndex = newAudioEncoder.dequeueInputBuffer(timeout)
            if (inputBufIndex >= 0) {
                // 检查pcm文件是否已经读取完毕(<0说明已经读到末尾)
                val sampleTime = pcmExtractor.sampleTime
                if (sampleTime < 0) {
                    // pcm读取完毕，编码一个完成的标记
                    newAudioEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    // 读取pcm数据
                    val flags = pcmExtractor.sampleFlags
                    val size = pcmExtractor.readSampleData(buf, 0)
                    val inputBuf = newAudioEncoder.getInputBuffer(inputBufIndex)
                    inputBuf?.let {
                        it.clear()
                        it.put(buf)
                        it.position(0)
                        // 通知dsp编码当前pcm数据
                        newAudioEncoder.queueInputBuffer(inputBufIndex, 0, size, sampleTime, flags)
                    }
                    // 通知pcmExtractor放弃当前内存数据，继续向下读新数据
                    pcmExtractor.advance()
                }
            }
            // 从dsp中读取编码后的音频数据
            var outBufIndex = newAudioEncoder.dequeueOutputBuffer(bufInfo, timeout)
            // 如果outBufIndex>=0才说明有数据未读取,有时不能一次性读取完毕，所以需要循环读取
            while (outBufIndex >= 0) {
                // 判断是否读取到了数据的末尾
                if (bufInfo.flags == BUFFER_FLAG_END_OF_STREAM) {
                    audioEncodeDone = true
                    break
                }

                // 通过outBufIndex这个索引，获取被编码后的数据所在的容器
                val encodedOutputBuf = newAudioEncoder.getOutputBuffer(outBufIndex)
                encodedOutputBuf?.let {
                    // 获取被编码后的新音频后，使用muxer写入
                    mediaMuxer.writeSampleData(muxerAudioIndex, encodedOutputBuf, bufInfo)
                    it.clear()
                }
                newAudioEncoder.releaseOutputBuffer(outBufIndex, false)
                outBufIndex = newAudioEncoder.dequeueOutputBuffer(bufInfo, timeout)
            }
        }
        mediaExtractor.unselectTrack(audioTrackIndex)
        buf.clear()
        // 终止并释放编码器
        newAudioEncoder.stop()
        newAudioEncoder.release()

        // 开始编码视频文件并写入到文件
        mediaExtractor.selectTrack(videoTrackIndex)
        // seek到startTime前的一个I帧处
        mediaExtractor.seekTo(startTime, SEEK_TO_NEXT_SYNC)
        // 获取视频轨中的最大帧的大小
        val maxBufSizeOfOriginVideo = videoFormat.getInteger(KEY_MAX_INPUT_SIZE)
        // 申请内存空间(复用之前定义的buf)
        buf = ByteBuffer.allocateDirect(maxBufSizeOfOriginVideo)
        // 开始从原视频文件中抽视频帧写入新文件
        while (true) {
            val sampleTime = mediaExtractor.sampleTime
            if (sampleTime == -1L) {
                // 文件读完
                break
            }
            if (sampleTime < startTime) {
                // 还没读取到制定的开始时间戳处,丢弃数据，继续向下读
                mediaExtractor.advance()
                continue
            }
            if (sampleTime > endTime) {
                // 指定时间段内的数据已经读取完毕
                break
            }
            // 复用之前定义的bufInfo来写入视频
            bufInfo.presentationTimeUs = sampleTime - startTime + 600
            bufInfo.flags = mediaExtractor.sampleFlags
            bufInfo.size = mediaExtractor.readSampleData(buf, 0)
            if (bufInfo.size < 0) {
                // 读取出错
                break
            }
            // 写入新文件
            mediaMuxer.writeSampleData(videoTrackIndex, buf, bufInfo)
            // 继续向下读取新数据
            mediaExtractor.advance()
        }
        // 操作完成，释放内存空间
        mediaMuxer.release()
        mediaExtractor.release()
        pcmExtractor.release()
    }

    /**
     * 混合两个pcm文件
     * */
    private fun mixPcm(pcm1Path: String, pcm2Path: String, vol1: Int, vol2: Int, resultPath: String) {
        val volume1 = vol1 / 100f * 1
        val volume2 = vol2 / 100f * 1
//待混音的两条数据流 还原   傅里叶  复杂
        //待混音的两条数据流 还原   傅里叶  复杂
        val is1 = FileInputStream(pcm1Path)
        val is2 = FileInputStream(pcm2Path)
        var end1 = false
        var end2 = false
//        输出的数据流
        //        输出的数据流
        val fileOutputStream = FileOutputStream(resultPath)
        val buffer1 = ByteArray(2048)
        val buffer2 = ByteArray(2048)
        val buffer3 = ByteArray(2048)
        var temp2: Short
        var temp1: Short
        while (!end1 || !end2) {
            if (!end2) {
                end2 = is2.read(buffer2) == -1
            }
            if (!end1) {
                end1 = is1.read(buffer1) == -1
            }
            var voice = 0
            //2个字节
            var i = 0
            while (i < buffer2.size) {
//前 低字节  1  后面低字节 2  声量值
//                32767         -32768
                temp1 = ((buffer1[i] and (0xff).toByte()).toInt() or (buffer1[i + 1] and (0xff).toByte()).toInt() shl 8).toShort()
                temp2 = ((buffer2[i] and (0xff).toByte()).toInt() or (buffer2[i + 1] and (0xff).toByte()).toInt() shl 8).toShort()
                voice = (temp1 * volume1 + temp2 * volume2).toInt()
                if (voice > 32767) {
                    voice = 32767
                } else if (voice < -32768) {
                    voice = -32768
                }
                buffer3[i] = (voice and 0xFF).toByte()
                buffer3[i + 1] = ((voice ushr 8) and 0xFF).toByte()
                i += 2
            }
            fileOutputStream.write(buffer3)
        }
        is1.close()
        is2.close()
        fileOutputStream.close()
    }

    /**
     * 从多媒体文件中截取音频并转化为PCM格式
     * @param mediaPath: 多媒体文件路径
     * @param output pcm的输出路径
     * @param startTime 时间起点
     * @param endTime 时间终点*/
    private fun decodePCM(
        mediaPath: String, output: String, startTime: Long, endTime: Long
    ) {
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
        mediaExtractor.seekTo(startTime, SEEK_TO_NEXT_SYNC)
        val audioFormat = mediaExtractor.getTrackFormat(audioTrack)
        val mediaCodec =
            audioFormat.getString(MediaFormat.KEY_MIME)?.let { MediaCodec.createDecoderByType(it) }
        mediaCodec?.configure(audioFormat, null, null, 0)
        mediaCodec?.start()
        // 如果包含最大输入空间的数据则取值
        val maxBufSize = if (audioFormat.containsKey(KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(KEY_MAX_INPUT_SIZE)
        } else {
            100 * 1024
        }
        // 准备pcm文件写入
        val pcmFile = File(output)
        val pcmFileChannel = FileOutputStream(pcmFile).channel
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

                // 从mediaExtractor中读取数据到buf中，准备需要解码的数据(此时是aac数据)
                bufInfo.size = mediaExtractor.readSampleData(buf, 0)
                bufInfo.presentationTimeUs = curSampleTime
                @SuppressLint("WrongConstant")
                bufInfo.flags = mediaExtractor.sampleFlags
//                val content = ByteArray(buf.remaining())
//                buf.get(content)
                val inputBuf = mediaCodec.getInputBuffer(inIndex)
                inputBuf?.put(buf)
                mediaCodec.queueInputBuffer(
                    inIndex, 0, bufInfo.size,
                    bufInfo.presentationTimeUs, bufInfo.flags
                )
                // 释放上一帧数据，否则不会走到下一帧
                mediaExtractor.advance()
            }
            // 获取解码后的数据(即pcm数据)
            var outIndex = mediaCodec?.dequeueOutputBuffer(bufInfo, timeout)
            // 可能一次解码不完全，所以搞一个循环
            while (outIndex != null && outIndex >= 0) {
                mediaCodec?.getOutputBuffer(outIndex)?.let {
                    pcmFileChannel.write(it)
                }
                mediaCodec?.releaseOutputBuffer(outIndex, false)
                outIndex = mediaCodec?.dequeueOutputBuffer(bufInfo, timeout)

            }
        }
        // 抽取音频并且解码完毕，释放资源
        pcmFileChannel.close()
        mediaExtractor.release()
        mediaCodec?.stop()
        mediaCodec?.release()
    }

    /**
     * @param audio 是否寻找音频轨*/
    private fun findTrack(extractor: MediaExtractor, type: MediaType = MediaType.Audio): Int {
        val trackCount = extractor.trackCount
        for (index in 0..trackCount) {
            // 获取当前媒体封装文件的轨道配置信息
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME)
            when (type) {
                MediaType.Audio -> {
                    if (mime?.startsWith("audio") == true) {
                        return index
                    }
                }
                MediaType.Video -> {
                    if (mime?.startsWith("video") == true) {
                        return index
                    }
                }
            }
        }
        return -1
    }
}