
#include <string>
#include "native-lib.h"

// 全局live对象，存储当前最新的sps和pps数据，以及rtmp实例
Live *live = nullptr;
int timeout = 10;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_h264encoderdemo_transmit_rtmp_RTMPSender_connect(
        JNIEnv *env, jobject thiz,
        jstring url_) {
    const char *url = env->GetStringUTFChars(url_, nullptr);
    int ret;
    do {
        size_t live_size = sizeof(Live);
        live = static_cast<Live *>(malloc(live_size));
        memset(live, 0, live_size);
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = timeout;
        LOGI("start connect %s", url);
        ret = RTMP_SetupURL(live->rtmp, (char *) url);
        if (!ret) {
            LOGE("RTMP_SetupURL error!");
            break;
        }
        RTMP_EnableWrite(live->rtmp);
        LOGI("RTMP connected.");
        ret = RTMP_Connect(live->rtmp, nullptr);
        if (!ret) {
            LOGE("RTMP_Connect error!");
            break;
        }
        LOGI("RTMP_ConnectStream");
        ret = RTMP_ConnectStream(live->rtmp, 0);
        if (!ret) {
            LOGE("RTMP_ConnectStream error!");
            break;
        }
        LOGI("connect success");
    } while (false);
    if (!ret && live) {
        free(live);
        live = nullptr;
    }
    env->ReleaseStringUTFChars(url_, url);
    return ret;
}

/**
 * 解析出data中的sps和pps数据，并复制到live实例中
 * */
int parseSPSPPS(int8_t *data, const int len, Live *localLive) {
    for (int i = 0; i < len; ++i) {
        // data数据格式为 0x00 0x00 0x00 0x01 0x67 sps 0x00 0x00 0x00 0x01 0x68 pps
        // 找到 sps 和 0x68间的分隔符
        if (i + 4 < len && data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00
            && data[i + 3] == 0x01 && data[i + 4] == 0x68) {
            // 解析出sps数据
            // 删除 0x00 0x00 0x00 0x01 分隔符，rtmp不需要
            localLive->sps_len = i - 4;
            localLive->sps = static_cast<int8_t *>(malloc(localLive->sps_len));
            // 拷贝0x67 sps
            memcpy(localLive->sps, data + 4, localLive->sps_len);
            // 解析出pps数据
            localLive->pps_len = len - (4 + localLive->sps_len) - 4;
            localLive->pps = static_cast<int8_t *>(malloc(localLive->pps_len));
            memcpy(localLive->pps, data + 4 + localLive->sps_len + 4, localLive->pps_len);
            LOGI("sps len:%d, pps len:%d", localLive->sps_len, localLive->pps_len);
            return 1;
        }
    }
    return 0;
}

/**
 * 构造sps/pps帧
 * */
RTMPPacket *buildSPSPPSPacket(Live *localLive) {
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    // 固定大小
    int body_size = 16 + localLive->sps_len + localLive->pps_len;
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    // sps
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    // version
    packet->m_body[i++] = 0x01;
    // profile: baseline main high
    packet->m_body[i++] = localLive->sps[1];
    // profile_compatibility  兼容性
    packet->m_body[i++] = localLive->sps[2];
    // level
    packet->m_body[i++] = localLive->sps[3];
    packet->m_body[i++] = 0xFF;
    // sps个数
    packet->m_body[i++] = 0xE1;
    // sps长度(因为m_body为char,sps_len为int16_t,所以使用位移操作取出对应字节上的数据进行赋值)
    packet->m_body[i++] = (localLive->sps_len >> 8) & 0xFF;
    packet->m_body[i++] = localLive->sps_len & 0xFF;
    // sps内容
    memcpy(&packet->m_body[i], localLive->sps, localLive->sps_len);
    i += localLive->sps_len;

    // pps
    // pps个数
    packet->m_body[i++] = 0x01;
    // pps长度
    packet->m_body[i++] = (localLive->pps_len >> 8) & 0xFF;
    packet->m_body[i++] = localLive->pps_len & 0xFF;
    // pps内容
    memcpy(&packet->m_body[i], localLive->pps, localLive->pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    // 一个固定值即可(要和之后的视频帧的channel保持一致，和音频帧区别开来)
    packet->m_nChannel = 0x04;
    // 是否使用绝对时间戳
    packet->m_hasAbsTimestamp = 0;
    // 时间戳如果自身不设置或设置为0，则rtmplib内部会自动设置
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = localLive->rtmp->m_stream_id;
    return packet;
}

/**
 * 构造视频帧的数据，I/P/B
 * */
 RTMPPacket *buildVideoPacket(int8_t *buf, int len, const long tms, Live *localLive) {
     // 先跳过0x00 0x00 0x00 0x01的分隔符;指针后移，长度减4
     buf += 4;
     len -= 4;
     RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
     // packet的body_size 为视频帧原数据长度 + 0x17/0x27 0x01 0x00 0x00 0x00 四字节的数据长度
     int body_size = len + 9;
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    // I帧，首字节为0x17； P/B帧，首字节为0x27
    if (buf[0] == 0x65) {
        packet->m_body[i++] = 0x17;
    } else {
        packet->m_body[i++] = 0x27;
    }
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    // 四字节的数据长度(因为m_body为char,len为int,所以使用位移操作取出对应字节上的数据进行赋值)
    packet->m_body[i++] = (len >> 24) & 0xFF;
    packet->m_body[i++] = (len >> 16) & 0xFF;
    packet->m_body[i++] = (len >> 8) & 0xFF;
    packet->m_body[i++] = len & 0xFF;
    // 拷贝视频数据
    memcpy(&(packet->m_body[i]), buf, len);
    // 配置packet其他数据项
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    // 一个固定值即可(要和之后的视频帧的channel保持一致，和音频帧区别开来)
    packet->m_nChannel = 0x04;
    // 设置无绝对时间戳，使用相对时间戳
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = tms;
    // 设置数据包大小级别
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    // TODO 这个字段是干嘛的？
    packet->m_nInfoField2 = localLive->rtmp->m_stream_id;
    return packet;
 }

/**
 * 发送RTMPPacket
 * 包括 sps/pps  I/P/B视频帧  音频帧
 * */
int sendPacket(Live *localLive, RTMPPacket *packet) {
    int ret = RTMP_SendPacket(localLive->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

int sendVideo(int8_t *buf, const int len, const long tms) {
    int ret;
    // 判断帧类型,0x67为sps
    if (buf[4] == 0x67) {
        // sps/pps
        if (live && (!live->sps || !live->pps)) {
            // 取出buf中的sps和pps数据
            ret = parseSPSPPS(buf, len, live);
        }
    } else {
        // 判断是否使关键帧
        if (buf[4] == 0x65) {
            // 发送关键帧之前，先发送sps/pps帧
            RTMPPacket  *packet = buildSPSPPSPacket(live);
            ret = sendPacket(live, packet);
            if (!ret) {
                LOGE("send sps/pps error!");
            }
        }
        // 视频帧的发送
        RTMPPacket  *packet = buildVideoPacket(buf, len, tms, live);
        ret = sendPacket(live, packet);
        if (!ret) {
            LOGE("send video error!");
        }
    }
    return ret;
}

RTMPPacket *buildAudioPacket(int8_t *buf,  const int len, const int type, const long tms, Live *localLive) {
    // 添加0xAF 0x00/0x01
    int body_size = len + 2;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, body_size);
    packet->m_body[0] = 0xAF;
    if (type == JAVA_RTMP_PACKET_TYPE_AUDIO_HEAD) {
        packet->m_body[1] = 0x00;
    } else {
        packet->m_body[1] = 0x01;
    }
    memcpy(&packet->m_body[2], buf, len);
    // 配置RTMPPacket的其他配置项
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = body_size;
    // 一个固定值即可(要和之后的音频帧的channel保持一致，和视频帧区别开来)
    packet->m_nChannel = 0x05;
    // 设置无绝对时间戳，使用相对时间戳
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = tms;
    // 设置数据包大小级别
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    // TODO 这个字段是干嘛的？
    packet->m_nInfoField2 = localLive->rtmp->m_stream_id;
    return packet;

}

int sendAudio(int8_t *buf, const int len, const int type, const long tms) {
    RTMPPacket *packet = buildAudioPacket(buf, len, type, tms, live);
    int ret = sendPacket(live, packet);
    if (!ret) {
        LOGE("send audio error!");
    }
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_h264encoderdemo_transmit_rtmp_RTMPSender_sendData(
        JNIEnv *env, jobject thiz,
        jbyteArray data_, jint len,
        jlong tms, jint type) {
    int ret = 0;
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    if (type == JAVA_RTMP_PACKET_TYPE_VIDEO) {
        ret = sendVideo(data, len, (long)tms);
    } else {
        ret = sendAudio(data, len, type, (long)tms);
    }
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}