//
// Created by Administrator on 2023/7/17.
//

#include "ConvertUtil.h"

ConvertUtil::ConvertUtil() {
}

ConvertUtil::~ConvertUtil() {
}

void ConvertUtil::shortToBytes(short num, unsigned char *bytes) {
    bytes[0] = (unsigned char)(num & 0xFF);
    bytes[1] = (unsigned char)((num >> 8) & 0xFF);
}

void ConvertUtil::shortArrayToByteArray(short *shortArray, int arrayLength, unsigned char *byteArray) {
    for (int i = 0; i < arrayLength; i++) {
        shortToBytes(shortArray[i], byteArray + (i * 2));
    }
}
