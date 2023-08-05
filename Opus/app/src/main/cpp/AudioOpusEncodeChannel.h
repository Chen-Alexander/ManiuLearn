//
// Created by Administrator on 2023/3/18.
//

#ifndef X264OPUSRTMP_AUDIOOPUSENCODECHANNEL_H
#define X264OPUSRTMP_AUDIOOPUSENCODECHANNEL_H

#include <cstdio>
#include "jni.h"
#include "rtmp.h"
#include "native-lib.h"
#include "opus.h"
#include "modules/audio_processing/legacy_ns/noise_suppression_x.h"
#include "ConvertUtil.h"

class AudioOpusEncodeChannel {

public:
    AudioOpusEncodeChannel(bool debug);
    ~AudioOpusEncodeChannel();
    // 设置编码参数
    void setAudioEncodeInfo(int sampleRate, int channelCount, int bitRate, int complexity, char *debugFilePath);
    void encodeData(short *data, int size);
    // Debug
    bool isDebug = false;

private:
    ConvertUtil convertUtil = ConvertUtil();
    OpusEncoder *opusEncoder = nullptr;
    NsxHandle *nsxHandle = nullptr;
    FILE *debug_original_fp = nullptr;
    FILE *debug_opus_fp = nullptr;
    FILE *debug_noise_fp = nullptr;
};

#endif //X264OPUSRTMP_AUDIOOPUSENCODECHANNEL_H
