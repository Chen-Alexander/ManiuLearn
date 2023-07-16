//
// Created by Administrator on 2023/7/1.
//

#include "AudioAACEncodeChannel.h"
#include "MyLog.h"

AudioAACEncodeChannel::AudioAACEncodeChannel(bool debug) {
    this->isDebug = debug;
}

/**
 * 检测该编码器是否支持该采样格式
 * */
int AudioAACEncodeChannel::check_sample_fmt(const AVCodec *codec, enum AVSampleFormat sample_fmt) {
    const enum AVSampleFormat *p = codec->sample_fmts;
    while (*p != AV_SAMPLE_FMT_NONE) {
        if (*p == sample_fmt) {
            return 1;
        }
        p++;
    }
    return 0;
}

/**
 * 检测该编码器是否支持该采样率
 * */
int AudioAACEncodeChannel::check_sample_rate(const AVCodec *codec, const int sample_rate) {
    const int *p = codec->supported_samplerates;
    while (*p != 0) {
        LOGI("%s support %dhz\n", codec->name, *p);
        if (*p == sample_rate) {
            return 1;
        }
        p++;
    }
    return 0;
}

/**
* 检测该编码器是否支持该采样率, 该函数只是作参考
* */
int AudioAACEncodeChannel::check_channel_layout(const AVCodec *codec,
                                                const uint64_t channel_layout) {
    const uint64_t  *p = codec->channel_layouts;
    if (!p) {
        LOGW("the codec %s no set channel_layouts\n", codec->name);
        return 1;
    }
    while (*p != 0) {
        LOGI("%s support channel_layout %llu\n", codec->name, *p);
        if (*p == channel_layout) {
            return 1;
        }
        p++;
    }
    return 0;
}

/**
 * 检测编码器的相关参数是否支持
 * */
int AudioAACEncodeChannel::check_codec(AVCodec *codec, AVCodecContext *codec_ctx) {
    int ret = check_sample_fmt(codec, codec_ctx->sample_fmt);
    if (!ret) {
        LOGE("Encoder does not support sample format %s", av_get_sample_fmt_name(codec_ctx->sample_fmt));
        return 0;
    }
    ret = check_sample_rate(codec, codec_ctx->sample_rate);
    if (!ret) {
        LOGE("Encoder does not support sample rate %d", codec_ctx->sample_rate);
        return 0;
    }
    ret = check_channel_layout(codec, codec_ctx->channel_layout);
    if (!ret) {
        LOGE("Encoder does not support channel layout %llu", codec_ctx->channel_layout);
        return 0;
    }
    LOGI("\n\nAudio encode config\n");
    LOGI("bit_rate:%lldkbps\n", codec_ctx->bit_rate / 1024);
    LOGI("sample_rate:%d\n", codec_ctx->sample_rate);
    LOGI("sample_fmt:%s\n", av_get_sample_fmt_name(codec_ctx->sample_fmt));
    LOGI("channels:%d\n", codec_ctx->channels);
    // frame_size 是在avcodec_open2后进行关联
    LOGI("1 frame_size:%d\n", codec_ctx->frame_size);
    return 1;
}

void AudioAACEncodeChannel::get_adts_header(AVCodecContext *ctx, uint8_t *adts_header, int aac_len) {
    //0: 96000 Hz  3: 48000 Hz 4: 44100 Hz
    uint8_t freq_idx = 0;
    switch (ctx->sample_rate) {
        case 96000: freq_idx = 0; break;
        case 88200: freq_idx = 1; break;
        case 64000: freq_idx = 2; break;
        case 48000: freq_idx = 3; break;
        case 44100: freq_idx = 4; break;
        case 32000: freq_idx = 5; break;
        case 24000: freq_idx = 6; break;
        case 22050: freq_idx = 7; break;
        case 16000: freq_idx = 8; break;
        case 12000: freq_idx = 9; break;
        case 11025: freq_idx = 10; break;
        case 8000: freq_idx = 11; break;
        case 7350: freq_idx = 12; break;
        default: freq_idx = 4; break;
    }
    uint8_t chanCfg = ctx->channels;
    uint32_t frame_length = aac_len + adts_header_len;
    adts_header[0] = 0xFF;
    adts_header[1] = 0xF1;
    adts_header[2] = ((ctx->profile) << 6) + (freq_idx << 2) + (chanCfg >> 2);
    adts_header[3] = (((chanCfg & 3) << 6) + (frame_length >> 11));
    adts_header[4] = ((frame_length & 0x7FF) >> 3);
    adts_header[5] = (((frame_length & 7) << 5) + 0x1F);
    adts_header[6] = 0xFC;
}

int AudioAACEncodeChannel::encode(AVCodecContext *ctx, AVFrame *frame, AVPacket *pkt, FILE *output) {
    int ret;

    // send the frame for encoding
    ret = avcodec_send_frame(ctx, frame);
    if (ret < 0) {
        LOGE("Error sending the frame to the encoder\n");
        return -1;
    }
    while (ret >= 0) {
        ret = avcodec_receive_packet(ctx, pkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            return 0;
        } else if (ret < 0) {
            LOGE("Error encoding audio frame\n");
            return -1;
        }
        uint8_t aac_header[adts_header_len];
        get_adts_header(ctx, aac_header, pkt->size);

        size_t len = 0;
        len = fwrite(aac_header, 1, adts_header_len, output);
        if (len != adts_header_len) {
            LOGE("fwrite aac_header failed\n");
            return -1;
        }
        len = fwrite(pkt->data, 1, pkt->size, output);
        if (len != pkt->size) {
            LOGE("fwrite aac data failed\n");
            return -1;
        }
        av_packet_unref(pkt);
    }
    return 1;
}

void AudioAACEncodeChannel::f32le_convert_to_fltp(float *f32le, float *fltp, int nb_samples) {
    // 左声道
    float *fltp_l = fltp;
    // 右声道
    float *fltp_r = fltp + nb_samples;
    // 左右左右左右...   0 1 2 3
    for (int i = 0; i < nb_samples; i++) {
        fltp_l[i] = f32le[i * 2];
        fltp_r[i] = f32le[i * 2 + 1];
    }
}

int AudioAACEncodeChannel::process(const char *in_pcm_file, const char *out_aac_file) {
    AVCodecID codec_id = AV_CODEC_ID_AAC;
    // find codec
    AVCodec *codec = avcodec_find_encoder(codec_id);
//    AVCodec *codec = avcodec_find_encoder_by_name("fdk_aac");
    if (!codec) {
        LOGE("Codec not found\n");
        return -1;
    }
    // alloc memory
    AVCodecContext *codec_ctx = avcodec_alloc_context3(codec);
    if (!codec_ctx) {
        LOGE("Could not allocate audio codec context\n");
        return -1;
    }
    codec_ctx->codec_id = codec->id;
    codec_ctx->codec_type = AVMEDIA_TYPE_AUDIO;
    codec_ctx->bit_rate = 32 * 1024;
    codec_ctx->channel_layout = AV_CH_LAYOUT_STEREO;
    codec_ctx->sample_rate = 48000;
    codec_ctx->channels = av_get_channel_layout_nb_channels(codec_ctx->channel_layout);
    codec_ctx->profile = FF_PROFILE_AAC_LOW;
    codec_ctx->sample_fmt = AV_SAMPLE_FMT_FLTP;
//    codec_ctx->sample_fmt = AV_SAMPLE_FMT_S16;
    // 检测支持采样格式支持情况
    int ret = check_codec(codec, codec_ctx);
    if (!ret) {
        return -1;
    }
    // 将编码器上下文和编码器进行关联
    ret = avcodec_open2(codec_ctx, codec, nullptr);
    if (ret < 0) {
        av_err2str(ret);
        LOGE("Could not open codec\n");
        return -1;
    }
    // 每一帧原始数据的有多少个采样点
    LOGI("2 frame_size:%d", codec_ctx->frame_size);
    // 打开输入和输出文件
    FILE *infile = fopen(in_pcm_file, "rb");
    if (!infile) {
        LOGE("Could not open %s\n", in_pcm_file);
        return -1;
    }
    FILE *outfile = fopen(out_aac_file, "wb");
    if (!outfile) {
        LOGE("Could not open %s\n", out_aac_file);
        return -1;
    }
    // 实例化packet和frame，并申请内存
    AVPacket *pkt = av_packet_alloc();
    if (!pkt) {
        LOGE("could not allocate the packet\n");
        return -1;
    }
    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        LOGE("Could not allocate audio frame\n");
        return -1;
    }
    /**
     * 每次送多少数据给编码器由：
     *  (1)frame_size(每帧单个通道的采样点数);
     *  (2)sample_fmt(采样点格式);
     *  (3)channel_layout(通道布局情况);
     * 3要素决定
     */
     frame->nb_samples = codec_ctx->frame_size;
     frame->format = codec_ctx->sample_fmt;
     frame->channel_layout = codec_ctx->channel_layout;
     frame->channels = codec_ctx->channels;
     LOGI("frame nb_samples:%d\n", frame->nb_samples);
     LOGI("frame sample_fmt:%d\n", frame->format);
     LOGI("frame channel_layout:%llu\n", frame->channel_layout);
     LOGI("frame channels_num:%d\n\n", frame->channels);
     // 为frame分配buffer
     ret = av_frame_get_buffer(frame, 0);
     if (ret < 0) {
         LOGE("Could not allocate audio data buffers\n");
         return -1;
     }
    // 循环读取数据；计算出每一帧的数据 单个采样点的字节 * 通道数目 * 每帧采样点数量
    int frame_bytes = av_get_bytes_per_sample(static_cast<AVSampleFormat>(frame->format))
            * frame->channels * frame->nb_samples;
    LOGI("frame_bytes %d\n", frame_bytes);
    auto *pcm_buf = static_cast<uint8_t *>(malloc(frame_bytes));
    if (!pcm_buf) {
        LOGE("pcm_buf malloc failed\n");
        return -1;
    }
    auto *pcm_temp_buf = static_cast<uint8_t *>(malloc(frame_bytes));
    if (!pcm_temp_buf) {
        LOGE("pcm_temp_buf malloc failed\n");
        return -1;
    }
    // 每个通道每个采样点的字节数
    int data_size = av_get_bytes_per_sample(static_cast<AVSampleFormat>(frame->format));
    int64_t pts = 0;
    LOGI("start encode\n");
    while (true) {
        memset(pcm_buf, 0, frame_bytes);
        size_t read_bytes = fread(pcm_buf, 1, frame_bytes, infile);
        if (read_bytes <= 0) {
            LOGI("read file finish\n");
            break;
        }

        ret = av_frame_make_writable(frame);
        if (ret < 0) {
            LOGE("av_frame_make_writable failed, ret = %d\n", ret);
            return -1;
        }
        // 填充音频帧
        if (AV_SAMPLE_FMT_S16 == frame->format) {
            LOGI("sample format is AV_SAMPLE_FMT_S16\n");
            ret = av_samples_fill_arrays(frame->data, frame->linesize,
                                         pcm_buf, frame->channels,
                                         frame->nb_samples,
                                         static_cast<AVSampleFormat>(frame->format), 0);
        } else {
            LOGI("sample format is not AV_SAMPLE_FMT_S16\n");
            memset(pcm_temp_buf, 0, frame_bytes);
            f32le_convert_to_fltp(reinterpret_cast<float *>(pcm_buf),
                                  reinterpret_cast<float *>(pcm_temp_buf),
                                  frame->nb_samples);
            ret = av_samples_fill_arrays(frame->data, frame->linesize,
                                         pcm_temp_buf, frame->channels,
                                         frame->nb_samples,
                                         static_cast<AVSampleFormat>(frame->format), 0);
        }
        if (ret < 0) {
            LOGE("av_samples_fill_arrays failed, ret = %d\n", ret);
            return -1;
        }
        // 计算pts
        pts += frame->nb_samples;
        // 使用采样率作为pts的单位，具体换算成秒 pts*1/采样率
        frame->pts = pts;
        ret = encode(codec_ctx, frame, pkt, outfile);
        if (!ret) {
            LOGE("encode failed\n");
            return -1;
        }
    }

    // flush codec
    encode(codec_ctx, frame, pkt, outfile);
    // close file
    fclose(infile);
    fclose(outfile);
    // 清理内存
    if (pcm_buf) {
        free(pcm_buf);
    }
    if (pcm_temp_buf) {
        free(pcm_temp_buf);
    }
    av_frame_free(&frame);
    av_packet_free(&pkt);
    avcodec_free_context(&codec_ctx);
    LOGI("encode finish.");
    return 0;
}

























