//
// Created by Administrator on 2023/1/15.
//

#ifndef SCREENSHARE_AUDIO_RTMP_NATIVE_LIB_H
#define SCREENSHARE_AUDIO_RTMP_NATIVE_LIB_H

#endif //SCREENSHARE_AUDIO_RTMP_NATIVE_LIB_H

#include <jni.h>

extern "C"
{
#include  "librtmp/rtmp.h"
}

#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"RTMP",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_INFO,"RTMP",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_INFO,"RTMP",__VA_ARGS__)

#define JAVA_RTMP_PACKET_TYPE_VIDEO 0
#define JAVA_RTMP_PACKET_TYPE_AUDIO_HEAD 1
#define JAVA_RTMP_PACKET_TYPE_AUDIO_DATA 2

typedef struct {
    int8_t *sps;
    int16_t sps_len;

    int8_t *pps;
    int16_t pps_len;

    RTMP *rtmp;
} Live;
