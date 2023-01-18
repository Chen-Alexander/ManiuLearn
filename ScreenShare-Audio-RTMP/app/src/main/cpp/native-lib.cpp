#include <jni.h>
#include <string>
extern "C"
{
    #include  "librtmp/rtmp.h"
}
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"David",__VA_ARGS__)

typedef struct {
    int8_t *sps;
    int16_t sps_len;

    int16_t pps_len;
    int8_t *pps;

    RTMP *rtmp;
} Live;
Live *live = nullptr;
void prepareVideo(int8_t *data, int len, Live *live) {
    for (int i = 0; i < len; i++) {
        //0x00 0x00 0x00 0x01
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                //0x00 0x00 0x00 0x01 7 sps 0x00 0x00 0x00 0x01 8 pps
                //将sps pps分开
                //找到pps
                if (data[i + 4]  == 0x68) {
                    //去掉界定符
                    live->sps_len = i - 4;
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                    memcpy(live->sps, data + 4, live->sps_len);

                    live->pps_len = len - (4 + live->sps_len) - 4;
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                    memcpy(live->pps, data + 4 + live->sps_len + 4, live->pps_len);
                    LOGI("sps:%d pps:%d", live->sps_len, live->pps_len);
                    break;
                }
            }
        }
    }
}

//第一次输出sps pps
//00 00 00 01   67  42C0298D680B40A1A01E1108D4   00   00  00  01    68CE01A835C8

//void prepareVideo(int8_t *data, int len, Live *live) {
//
//    for (int i = 0; i < len; i++) {
//        if (data[i] == 0x00 && data[i + 1] == 0x00
//            && data[i + 2] == 0x00
//            && data[i + 3] == 0x01&&  data[i + 4] == 0x68){
//            LOGI("david--------------> 找到sps pps  type " );
////            计算sps长度
//            live->sps_len = i - 4;
////            根据长度   初始化容器
//            live->sps = static_cast<int8_t *>(malloc(live->sps_len));
////            copy sps内存到容器
//            memcpy(live->sps, data + 4, live->sps_len);
//
////PPS
//            live->pps_len = len - i - 4;
//            live->pps = static_cast<int8_t *>(malloc(live->pps_len));
//            memcpy(live->pps, data +i + 4, live->pps_len);
//            break;
//        }
//    }
//}

    RTMPPacket *createVideoPackage(Live *live) {
        RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
//        每个数据包 肯定
        int body_size = 16 + live->sps_len + live->pps_len;
//I  P  B
//sps pps
        RTMPPacket_Alloc(packet, body_size);

        int i = 0;
        packet->m_body[i++] = 0x17;
        packet->m_body[i++] = 0x00;
        //CompositionTime
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;
        //AVC sequence header
        packet->m_body[i++] = 0x01;

        packet->m_body[i++] = live->sps[1]; //profile 如baseline、main、 high

        packet->m_body[i++] = live->sps[2]; //profile_compatibility 兼容性
        packet->m_body[i++] = live->sps[3]; //  level
//固定写法
        packet->m_body[i++] = 0xFF;
        packet->m_body[i++] = 0xE1;
//        sps 长度  2个字节
//        live->sps_len  ===、2个字节
        packet->m_body[i++] = (live->sps_len >> 8) & 0xFF;
        packet->m_body[i++] = live->sps_len & 0xFF;
//         67  42C0298D680B40A1A01E1108D4
        memcpy(&packet->m_body[i], live->sps, live->sps_len);
//        拷贝 i++后面去 方便后面继续赋值
        i += live->sps_len;
        packet->m_body[i++] = 0x01;



        //pps length
        packet->m_body[i++] = (live->pps_len >> 8) & 0xff;
        packet->m_body[i++] = live->pps_len & 0xff;
        memcpy(&packet->m_body[i], live->pps, live->pps_len);

//        拼接成功       配置 直播怎么做到 配置
        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
        packet->m_nBodySize = body_size;
        packet->m_nChannel = 0x04;
        packet->m_hasAbsTimestamp = 0;
//        系统赋值
        packet->m_nTimeStamp = 0;
//        数据包大小  给服务器看的
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        packet->m_nInfoField2 = live->rtmp->m_stream_id;
        return packet;
//        昨天 周一
    }


int sendPacket(RTMPPacket *packet) {
    int r = RTMP_SendPacket(live->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    free(packet);
    return r;


}
//00 00 00 01 65 B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
RTMPPacket *createVideoPackage(int8_t *buf, int len, const long tms, Live *live) {
//
    buf += 4;
    len -= 4;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    int body_size = 9 +len;
    RTMPPacket_Alloc(packet, body_size);
    if (buf[0] == 0x65) {
        packet->m_body[0] = 0x17;
    } else{
        packet->m_body[0] = 0x27;
    }
    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //长度
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;
    //数据
    memcpy(&packet->m_body[9], buf, len);


//        拼接成功       配置 直播怎么做到 配置
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
//        系统赋值
    packet->m_nTimeStamp = tms;
//        数据包大小  给服务器看的
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;

}
//0000000141B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
//RTMPPacket *createVideoPackage1(int8_t *buf, int len, const long tms, Live *live) {
//    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
//    int body_size = 9 +len;
//    RTMPPacket_Alloc(packet, body_size);
//    packet->m_body[0] = 0x27;
//    packet->m_body[1] = 0x01;
//    packet->m_body[2] = 0x00;
//    packet->m_body[3] = 0x00;
//    packet->m_body[4] = 0x00;
//    //长度
//    packet->m_body[5] = (len >> 24) & 0xff;
//    packet->m_body[6] = (len >> 16) & 0xff;
//    packet->m_body[7] = (len >> 8) & 0xff;
//    packet->m_body[8] = (len) & 0xff;
//    //数据
//    memcpy(&packet->m_body[9], buf, len);
//
//
////        拼接成功       配置 直播怎么做到 配置
//    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
//    packet->m_nBodySize = body_size;
//    packet->m_nChannel = 0x04;
//    packet->m_hasAbsTimestamp = 0;
////        系统赋值
//    packet->m_nTimeStamp = tms;
////        数据包大小  给服务器看的
//    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
//    packet->m_nInfoField2 = live->rtmp->m_stream_id;
//    return packet;
//
//}
//00 00 00 01   67  42C0298D680B40A1A01E1108D4   00   00  00  01    68CE01A835C8
//0000000165B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
//int sendVideo(int8_t *buf, int len, long tms) {
//    //sps pps
//    int ret;
//    int type = (buf[4] & 0x1F);
//    LOGI("david-------------->type  %d",type);
//    if (buf[4] == 0x67) {
//        LOGI("david-------------->sps  type  %d", (!live->pps || !live->sps));
//        if (live && ((!live->pps) || (!live->sps))) {
//            LOGI("david-------------->sps prepareVideo");
//            prepareVideo(buf, len, live);
//        }
//        return ret;
//    }
////    I帧
//    if (buf[4] == 0x65) {
////        发两帧  一帧sps  一帧 I帧
////拼接Packet
//        RTMPPacket *packet = createVideoPackage(live);
//        sendPacket(packet);
//
//    }
//
//    //拼接I帧
//    RTMPPacket *packet1 = createVideoPackage(buf, len, tms, live);
////发送I帧
//    sendPacket(packet1);
//    LOGI("----->发送  ");
//    return ret;
//}

RTMPPacket *createAudioPacket(int8_t *buf, const int len, const int type, const long tms,
                              Live *live) {
    int body_size = len + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    packet->m_body[0] = 0xAF;
    if (type == 1) {
        packet->m_body[1] = 0x00;
    } else{
        packet->m_body[1] = 0x01;
    }

    memcpy(&packet->m_body[2], buf, len);


    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x05;
    packet->m_nBodySize = body_size;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;

}
int sendAudio(int8_t *buf, int len, int type, int tms) {
//    创建音频包   如何组装音频包
    RTMPPacket *packet = createAudioPacket(buf, len, type, tms, live);
    int ret=sendPacket(packet);
    return ret;
}


int sendVideo(int8_t *buf, int len, long tms) {
    int ret;
    if (buf[4] == 0x67) {//sps pps
        if (live && (!live->pps || !live->sps)) {
            prepareVideo(buf, len, live);
        }
    } else {
        if (buf[4] == 0x65) {//关键帧
            RTMPPacket *packet = createVideoPackage(live);
            if (!(ret = sendPacket(packet))) {
            }
        }
        RTMPPacket *packet = createVideoPackage(buf, len, tms, live);
        ret = sendPacket(packet);
    }
    return ret;
}
extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                 jint len, jlong tms,jint type) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    switch (type) {
        case 0: //video
            ret = sendVideo(data, len, tms);
            break;
        default: //audio
            ret = sendAudio(data, len, type, tms);
            LOGI("send Audio  lenght :%d", len);
            break;
    }
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}
//手机硬件   手机 模拟器  H265
extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
//能1  不能2 MK  2.3
    const char *url = env->GetStringUTFChars(url_, 0);
    int ret;
    do {
        live = (Live*)malloc(sizeof(Live));
        memset(live, 0, sizeof(Live));
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        LOGI("connect %s", url);
        if (!(ret = RTMP_SetupURL(live->rtmp, (char*)url))) break;
        RTMP_EnableWrite(live->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) break;
        LOGI("connect success");
    } while (0);
    if (!ret && live) {
        free(live);
        live = nullptr;
    }

    env->ReleaseStringUTFChars(url_, url);
    return ret;
}
