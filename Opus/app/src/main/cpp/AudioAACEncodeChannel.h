//
// Created by Administrator on 2023/7/1.
//

#ifndef OPUS_AUDIOAACENCODECHANNEL_H
#define OPUS_AUDIOAACENCODECHANNEL_H

#include <cstdio>
#include <cstdint>
#include <cstdlib>

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavutil/channel_layout.h"
#include "libavutil/common.h"
#include "libavutil/frame.h"
#include "libavutil/samplefmt.h"
#include "libavutil/opt.h"
}

class AudioAACEncodeChannel {

public:
    AudioAACEncodeChannel(bool debug);

    ~AudioAACEncodeChannel();

    // Debug
    bool isDebug = false;

    int process(const char* in_pcm_file, const char* out_aac_file);

private:
    int adts_header_len = 7;

    int check_sample_fmt(const AVCodec* codec, enum AVSampleFormat sample_fmt);

    int check_sample_rate(const AVCodec* codec, const int sample_rate);

    int check_channel_layout(const AVCodec* codec, const uint64_t channel_layout);

    int check_codec(AVCodec* codec, AVCodecContext* codec_ctx);

    void get_adts_header(AVCodecContext* ctx, uint8_t* adts_header, int aac_len);

    int encode(AVCodecContext* ctx, AVFrame* frame, AVPacket* pkt, FILE* output);

    void f32le_convert_to_fltp(float* f32le, float* fltp, int nb_samples);
};

#endif //OPUS_AUDIOAACENCODECHANNEL_H