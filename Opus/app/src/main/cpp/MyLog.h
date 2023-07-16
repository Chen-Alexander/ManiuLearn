//
// Created by Administrator on 2023/2/19.
//

#ifndef X264OPUSRTMP_MYLOG_H
#define X264OPUSRTMP_MYLOG_H

#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"X264OPUS",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"X264OPUS",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,"X264OPUS",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"X264OPUS",__VA_ARGS__)

#endif //X264OPUSRTMP_MYLOG_H
