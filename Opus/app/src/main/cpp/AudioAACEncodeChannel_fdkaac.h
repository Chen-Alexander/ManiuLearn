//
// Created by Administrator on 2023/3/18.
//

#ifndef X264OPUSRTMP_AUDIOAACENCODECHANNEL_H
#define X264OPUSRTMP_AUDIOAACENCODECHANNEL_H

#include <cstdio>
#include "jni.h"
#include "rtmp.h"
#include "native-lib.h"
#include "modules/audio_processing/legacy_ns/noise_suppression_x.h"
#include "faac-1_29_9_2/include/faac.h"

class AudioAACEncodeChannel_fdkaac {

public:
    AudioAACEncodeChannel_fdkaac(bool debug);

    ~AudioAACEncodeChannel_fdkaac();

    // 设置编码参数
    void setAudioEncodeInfo(int sampleRate, int channelCount, char *debugFilePath);

    void encodeData(int32_t *data, int size);



    int getInputByteNum() {
        return inputByteNum;
    }

    // Debug
    bool isDebug = false;

private:
    faacEncHandle codec = nullptr;
    // 音频压缩成aac后最大数据量
    unsigned long maxOutputBytes;
    // 输出的数据缓冲区
    unsigned char *outputBuffer = nullptr;
    // 输入容器的大小
    unsigned long inputByteNum;
    NsxHandle *nsxHandle = nullptr;
    FILE *debug_pcm_fp = nullptr;
    FILE *debug_aac_fp = nullptr;
    FILE *debug_noise_fp = nullptr;

    unsigned long getAudioConfig(unsigned char* buf);
};

#endif //X264OPUSRTMP_AUDIOAACENCODECHANNEL_H
