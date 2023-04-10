//
// Created by Administrator on 2023/3/30.
//

#include "AudioOpusDecodeChannel.h"
#include "MyLog.h"

AudioOpusDecodeChannel::AudioOpusDecodeChannel(bool debug) {
    this->isDebug = debug;
}

AudioOpusDecodeChannel::~AudioOpusDecodeChannel() {
    opus_decoder_destroy(opusDecoder);
}

void AudioOpusDecodeChannel::setDecodeInfo(int sample_rate, int channel_count) {
    int error;
    opusDecoder = opus_decoder_create(sample_rate, channel_count, &error);
    if (error) {
        LOGE("create opus decoder error!");
        return;
    }
}

int AudioOpusDecodeChannel::decodeData(unsigned char *encoded, size_t encoded_size, short *decoded,
                                        size_t decoded_size) {
    int out_size = opus_decode(opusDecoder, encoded, encoded_size, decoded,
                          decoded_size, 0);
    return out_size;
}
