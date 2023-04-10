//
// Created by Administrator on 2023/3/30.
//

#ifndef X264OPUSRTMP_AUDIOOPUSDECODECHANNEL_H
#define X264OPUSRTMP_AUDIOOPUSDECODECHANNEL_H

#include <cstdio>
#include "jni.h"
#include "rtmp.h"
#include "native-lib.h"
#include "opus.h"

class AudioOpusDecodeChannel {
public:
    AudioOpusDecodeChannel(bool debug);
    ~AudioOpusDecodeChannel();
    void setDecodeInfo(int sample_rate, int channel_count);
    int decodeData(unsigned char *encoded, size_t encoded_size, short* decoded, size_t decoded_size);
    // Debug
    bool isDebug = false;

private:
    OpusDecoder *opusDecoder = nullptr;
};

#endif //X264OPUSRTMP_AUDIOOPUSDECODECHANNEL_H
