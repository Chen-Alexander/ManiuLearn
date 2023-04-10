//
// Created by Administrator on 2023/3/18.
//

#include <cstdlib>
#include "AudioOpusEncodeChannel.h"
#include "MyLog.h"
#include "string.h"

AudioOpusEncodeChannel::AudioOpusEncodeChannel(bool debug) {
    // 注意
    // WebRtcNsx_Create是定点数运算降噪
    // WebRtcNs_Create是浮点数运算降噪
    nsxHandle = WebRtcNsx_Create();
    this->isDebug = debug;
}

AudioOpusEncodeChannel::~AudioOpusEncodeChannel() {
    opus_encoder_destroy(opusEncoder);
    WebRtcNsx_Free(nsxHandle);
    /** Debug */
    if (this->isDebug) {
        if (debug_noise_fp != nullptr) {
            // Close the file
            fclose(debug_noise_fp);
        }
        if (debug_opus_fp != nullptr) {
            // Close the file
            fclose(debug_opus_fp);
        }
    }
    /** Debug */
}

/**
 * @param complexity 编码复杂度  [0, 10]
 * */
void AudioOpusEncodeChannel::setAudioEncodeInfo(int sampleRate, int channelCount, int bitRate, int complexity, char *debugFilePath) {
    int error;
    opusEncoder = opus_encoder_create(sampleRate, channelCount,
                                      OPUS_APPLICATION_RESTRICTED_LOWDELAY, &error);
    if (error != 0) {
        LOGE("create opus encoder error:%d", error);
        exit(1);
    }
    if (opusEncoder == nullptr) {
        LOGE("opusEncoder is nullptr!");
        exit(1);
    }
    // 0: CBR 1: VBR
    opus_encoder_ctl(opusEncoder, OPUS_SET_VBR(0));
    opus_encoder_ctl(opusEncoder, OPUS_SET_VBR_CONSTRAINT(true));
    opus_encoder_ctl(opusEncoder, OPUS_SET_BITRATE(bitRate));
    opus_encoder_ctl(opusEncoder, OPUS_SET_COMPLEXITY(complexity));
    opus_encoder_ctl(opusEncoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
    opus_encoder_ctl(opusEncoder, OPUS_SET_LSB_DEPTH(16));
    opus_encoder_ctl(opusEncoder, OPUS_SET_DTX(0));
    opus_encoder_ctl(opusEncoder, OPUS_SET_INBAND_FEC(0));
    opus_encoder_ctl(opusEncoder, OPUS_SET_PACKET_LOSS_PERC(0));
    if (nsxHandle == nullptr) {
        LOGE("nsxHandle is nullptr!");
        exit(1);
    }
    // fs仅支持 8000 16000 32000 48000
    int ret = WebRtcNsx_Init(nsxHandle, sampleRate);
    // mode 0: Mild, 1: Medium , 2: Aggressive, 3: high  数值越高，降噪越明显
    ret = WebRtcNsx_set_policy(nsxHandle, 2);
    /** Debug */
    if (this->isDebug) {
        char *local_debugFilePath = static_cast<char *>(malloc(strlen(debugFilePath)));
        strcpy(local_debugFilePath, debugFilePath);
        const char *noisePcmFileName = strcat(strcat(local_debugFilePath, "_noise"), ".pcm");
        // Delete the file if it exists
        remove(noisePcmFileName);
        // Open the file for writing in binary mode (append mode)
        debug_noise_fp = fopen(noisePcmFileName, "ab");
        if (debug_noise_fp == nullptr) {
            fprintf(stderr, "Error opening pcm file %s\n", noisePcmFileName);
            exit(1);
        }
        const char *opus_suffix_str = ".opus";
        const char *fileName = strcat(debugFilePath, opus_suffix_str);
        // Delete the file if it exists
        remove(fileName);
        // Open the file for writing in binary mode (append mode)
        debug_opus_fp = fopen(fileName, "ab");
        if (debug_opus_fp == nullptr) {
            fprintf(stderr, "Error opening opus file %s\n", fileName);
            exit(1);
        }
    }
    /** Debug */
}

void AudioOpusEncodeChannel::encodeData(short *data, int size) {
//    auto *noise_data = static_cast<short *>(malloc(size));
    // 采样率和speechFrame输就长度的对应值  8k:80  16k:160  32k:320
    // 所以，采样率时16k的话，此处就需要20ms的数据，这样，data在这个short数组长度就是160
//    WebRtcNsx_Process(nsxHandle, &data, 1, &noise_data);
    int out_size = size / 8;
    auto *encode_out_bytes = static_cast<unsigned char *>(malloc(out_size));
    int real_out_size = opus_encode(opusEncoder, data, size,
                                    encode_out_bytes,
                                    out_size);
    if (real_out_size < 0) {
        LOGE("opus_encode error:%d", real_out_size);
    }
    /** Debug */
    if (this->isDebug) {
        // Write data to the file
//        fwrite(noise_data, 1, size, debug_noise_fp);
//        printf("noise data written.");
        fwrite(encode_out_bytes, 1, real_out_size, debug_opus_fp);
        printf("Data written.");
    }
    /** Debug */
//    free(noise_data);
    free(encode_out_bytes);
}