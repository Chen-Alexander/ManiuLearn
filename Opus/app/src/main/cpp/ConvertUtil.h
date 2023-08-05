//
// Created by Administrator on 2023/7/17.
//

#ifndef OPUS_CONVERTUTIL_H
#define OPUS_CONVERTUTIL_H

class ConvertUtil {
public:
    ConvertUtil();
    ~ConvertUtil();
    void shortArrayToByteArray(short* shortArray, int arrayLength, unsigned char* byteArray);
private:
    void shortToBytes(short num, unsigned char* bytes);
};

#endif //OPUS_CONVERTUTIL_H
