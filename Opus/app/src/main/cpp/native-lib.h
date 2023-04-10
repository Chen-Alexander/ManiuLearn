//
// Created by Administrator on 2023/3/17.
//

#include <jni.h>
#include "opus.h"

#ifndef X264OPUSRTMP_NATIVE_LIB_H
#define X264OPUSRTMP_NATIVE_LIB_H

typedef struct {
    uint8_t *sps;
    int sps_len;
    uint8_t *pps;
    int pps_len;
} VideoConfig;

#endif //X264OPUSRTMP_NATIVE_LIB_H
