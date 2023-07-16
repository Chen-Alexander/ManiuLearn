//
// Created by Administrator on 2023/3/18.
//

#include <cstdlib>
#include "AudioAACEncodeChannel_fdkaac.h"
#include "MyLog.h"
#include "string.h"

AudioAACEncodeChannel_fdkaac::AudioAACEncodeChannel_fdkaac(bool debug) {
    // 注意
    // WebRtcNsx_Create是定点数运算降噪
    // WebRtcNs_Create是浮点数运算降噪
    nsxHandle = WebRtcNsx_Create();
    this->isDebug = debug;
}

AudioAACEncodeChannel_fdkaac::~AudioAACEncodeChannel_fdkaac() {
    faacEncClose(codec);
    WebRtcNsx_Free(nsxHandle);
    /** Debug */
    if (this->isDebug) {
        if (debug_noise_fp != nullptr) {
            // Close the file
            fclose(debug_noise_fp);
        }
        if (debug_aac_fp != nullptr) {
            // Close the file
            fclose(debug_aac_fp);
        }
    }
    /** Debug */
}

/**
 * @param complexity 编码复杂度  [0, 10]
 * */
void AudioAACEncodeChannel_fdkaac::setAudioEncodeInfo(int sampleRate, int channelCount, char *debugFilePath) {
    int error;
    unsigned long inputSamples;
    codec = faacEncOpen(sampleRate, channelCount, &inputSamples, &maxOutputBytes);
    if (codec == nullptr) {
        LOGE("codec instantiation failed!");
        exit(1);
    }
    inputByteNum = inputSamples * 2;
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));
    LOGI("Initialization, codec: %d, inputByteNum: %lu, maxOutputBytes: %lu", codec, inputByteNum, maxOutputBytes);
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(codec);
    // 指定版本 MPEG AAC
    configurationPtr->mpegVersion = MPEG4;
    configurationPtr->aacObjectType = LOW;
    // 设置输出AAC裸流数据，也可以设置输出ADTS数据
//    configurationPtr->outputFormat = 0;
    configurationPtr->outputFormat = 1;
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;
    error = faacEncSetConfiguration(codec, configurationPtr);
    if (error != 1) {
        LOGE("use configurationPtr failed!");
        exit(1);
    }
//    unsigned char* header;
//    unsigned long len = 0L;
//    faacEncGetDecoderSpecificInfo(codec, &header, &len);
//    if (header == nullptr || len <= 0) {
//        LOGE("faacEncGetDecoderSpecificInfo error!");
//        exit(1);
//    }
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
        const char *originalPcmFileName = strcat(local_debugFilePath, ".pcm");
        // Delete the file if it exists
        remove(originalPcmFileName);
        // Open the file for writing in binary mode (append mode)
        debug_pcm_fp = fopen(originalPcmFileName, "ab");
        if (debug_pcm_fp == nullptr) {
            fprintf(stderr, "Error opening original pcm file %s\n", originalPcmFileName);
            exit(1);
        }
//        const char *noisePcmFileName = strcat(strcat(local_debugFilePath, "_noise"), ".pcm");
//        // Delete the file if it exists
//        remove(noisePcmFileName);
//        // Open the file for writing in binary mode (append mode)
//        debug_noise_fp = fopen(noisePcmFileName, "ab");
//        if (debug_noise_fp == nullptr) {
//            fprintf(stderr, "Error opening noise pcm file %s\n", noisePcmFileName);
//            exit(1);
//        }
        const char *aac_suffix_str = ".aac";
        const char *fileName = strcat(debugFilePath, aac_suffix_str);
        // Delete the file if it exists
        remove(fileName);
        // Open the file for writing in binary mode (append mode)
        debug_aac_fp = fopen(fileName, "ab");
        if (debug_aac_fp == nullptr) {
            fprintf(stderr, "Error opening opus file %s\n", fileName);
            exit(1);
        }
//        fwrite(header, len, 1, debug_aac_fp);
    }
    /** Debug */
}

void AudioAACEncodeChannel_fdkaac::encodeData(int32_t *data, int size) {
    auto *noise_data = static_cast<short *>(malloc(size));
    // 采样率和speechFrame输就长度的对应值  8k:80  16k:160  32k:320
    // 所以，采样率时16k的话，此处就需要20ms的数据，这样，data在这个short数组长度就是160
//    WebRtcNsx_Process(nsxHandle, &data, 1, &noise_data);
    auto *encode_out_bytes = static_cast<unsigned char *>(malloc(maxOutputBytes));
    int real_out_size = faacEncEncode(codec, data,
                                      size, outputBuffer, maxOutputBytes);
    memcpy(encode_out_bytes, outputBuffer, real_out_size);
    if (real_out_size <= 0) {
        LOGE("faacEncEncode error:%d", real_out_size);
        free(noise_data);
        free(encode_out_bytes);
        return;
    }
    /** Debug */
    if (this->isDebug) {
        // Write data to the file
        fwrite(data, size, 1, debug_pcm_fp);
        printf("noise data written.");
//        fwrite(noise_data, size, 1, debug_noise_fp);
//        printf("noise data written.");
        fwrite(encode_out_bytes, real_out_size, 1, debug_aac_fp);
        printf("Data written.");
    }
    /** Debug */
    free(noise_data);
    free(encode_out_bytes);
}

unsigned long AudioAACEncodeChannel_fdkaac::getAudioConfig(unsigned char* buf){
    unsigned long len = 0L;
    faacEncGetDecoderSpecificInfo(codec, &buf, &len);
    if (buf == nullptr || len <= 0) {
        LOGE("faacEncGetDecoderSpecificInfo error!");
        exit(1);
    }
    return len;
}