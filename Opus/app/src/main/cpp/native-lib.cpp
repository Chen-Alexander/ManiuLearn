#include "native-lib.h"
#include <string>
#include "safe_queue.h"
#include "rtmp.h"
#include "AudioOpusEncodeChannel.h"
#include "AudioAACEncodeChannel_fdkaac.h"
#include "AudioOpusDecodeChannel.h"
#include "MyLog.h"
#include "modules/audio_processing/legacy_ns/noise_suppression_x.h"
#include "AudioAACEncodeChannel.h"

// 生产消费队列
SafeQueue<RTMPPacket *> packets;
// 是否开始工作的标记
bool isStart = false;
AudioOpusEncodeChannel *opusEncodeChannel;
AudioAACEncodeChannel_fdkaac *aacEncodeChannel;
//AudioAACEncodeChannel *aacEncodeChannel;
JavaVM *javaVm = nullptr;
JNIEnv *jniEnv = nullptr;
pthread_t pid;
uint32_t start_time;
AudioOpusDecodeChannel *opusDecodeChannel;
const char *default_debug_path = "/data/data/com.alexander.x264opusrtmp/files/audio.opus";

// 往队列存放数据的函数
void callbackPacket(RTMPPacket *packet) {
    if (packet != nullptr) {
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

// 释放packet所占资源
void releasePacket(RTMPPacket *packet) {
    if (packet != nullptr) {
        RTMPPacket_Free(packet);
        free(packet);
        packet = nullptr;
    }
}

// 子线程函体，从队列中获取数据
void *startFunc(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp = nullptr;
    do {
        rtmp = RTMP_Alloc();
        if (rtmp == nullptr) {
            LOGE("rtmp创建失败！");
            break;
        }
        RTMP_Init(rtmp);
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if (ret < 0) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        // 开启输出模式
        RTMP_EnableWrite(rtmp);
        // 连接服务器
        ret = RTMP_Connect(rtmp, 0);
        if (ret < 0) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (ret < 0) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }
        // 标记队列开始工作
        packets.setWork(1);
        RTMPPacket *packet = nullptr;
        // 记录开始时间，方便之后计算每一帧的timeStamp
        start_time = RTMP_GetTime();
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (packet == nullptr) {
                break;
            }
            // 记录流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            // 加入发送队列进行发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            if (ret < 0) {
                LOGE("发送数据失败");
                break;
            }
        }
        // 兜底释放
        releasePacket(packet);
    } while (false);
    if (rtmp != nullptr) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    // TODO 切记要家返回值 否则函数执行完成，回收的时候会崩溃；signal 4 (SIGILL), code 1 (ILL_ILLOPC) , fault addr 0xXXXXXXX
    return nullptr;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = vm->GetEnv(reinterpret_cast<void **>(&jniEnv), JNI_VERSION_1_4);
    if (result != JNI_OK) {
        LOGE("GetEnv error: %d", result);
        return result;
    }
    javaVm = vm;
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1init(JNIEnv *env, jobject thiz, jboolean isDebug) {
    opusEncodeChannel = new AudioOpusEncodeChannel(isDebug);
    opusDecodeChannel = new AudioOpusDecodeChannel(isDebug);
    aacEncodeChannel = new AudioAACEncodeChannel_fdkaac(isDebug);
//    aacEncodeChannel = new AudioAACEncodeChannel(isDebug);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path_) {
    if (isStart) {
        return;
    }
    // 获取服务器地址
    const char *path = env->GetStringUTFChars(path_, nullptr);
//    char *url = new char[strlen(path) + 1];
    char *url = new char[strlen(path)];
    strcpy(url, path);
    isStart = true;
    // 创建子线程去队列中读取RTMPPacket
    pthread_create(&pid, nullptr, startFunc, url);
    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1setOpusEncInfo(JNIEnv *env, jobject thiz,
                                                                  jint sample_rate,
                                                                  jint channel_count,
                                                                  jint bit_rate,
                                                                  jint complexity,
                                                                  jstring debugPath_) {
    char *debugPath = const_cast<char *>(env->GetStringUTFChars(debugPath_, 0));
    if (opusEncodeChannel->isDebug && strlen(debugPath) < 0) {
        strcpy(debugPath, default_debug_path);
    }
    if (opusEncodeChannel != nullptr) {
        opusEncodeChannel->setAudioEncodeInfo(sample_rate, channel_count,
                                              bit_rate, complexity,
                                              debugPath);
    }
    env->ReleaseStringUTFChars(debugPath_, debugPath);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1pushOpusAudio(JNIEnv *env, jobject thiz,
                                                                 jshortArray data_) {
    if (opusEncodeChannel == nullptr) {
        LOGE("opusEncodeChannel is null!");
        return;
    }
    jshort *data = env->GetShortArrayElements(data_, nullptr);
    jint size = env->GetArrayLength(data_);
    opusEncodeChannel->encodeData(data, size);
    env->ReleaseShortArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1setAACAudioEncInfo(JNIEnv *env, jobject thiz,
                                                                      jint sample_rate,
                                                                      jint channel_count,
                                                                      jstring debug_path) {
    if (aacEncodeChannel == nullptr) {
        LOGE("aacEncodeChannel is null!");
        return 0;
    }
    char * debugPath = const_cast<char *>(env->GetStringUTFChars(debug_path,
                                                                 nullptr));
    aacEncodeChannel->setAudioEncodeInfo(sample_rate, channel_count,
                                         debugPath);

//    aacEncodeChannel->process("/data/data/com.alexander.x264opusrtmp/files/audio.pcm",
//                              "/data/data/com.alexander.x264opusrtmp/files/audio.aac");
    env->ReleaseStringUTFChars(debug_path, debugPath);
//    return aacEncodeChannel->getInputByteNum();
    return 0;
}

//extern "C"
//JNIEXPORT void JNICALL
//Java_com_alexander_x264opusrtmp_LivePusher_native_1pushAACAudio(JNIEnv *env, jobject thiz,
//                                                                jshortArray data_) {
//    if (aacEncodeChannel == nullptr) {
//        LOGE("aacEncodeChannel is null!");
//        return;
//    }
//    short *data = env->GetShortArrayElements(data_, nullptr);
//    jint size = env->GetArrayLength(data_);
//    aacEncodeChannel->encodeData(data, size);
//    env->ReleaseShortArrayElements(data_, data, 0);
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1pushAACAudio(JNIEnv *env, jobject thiz,
                                                                jbyteArray data_) {
    if (aacEncodeChannel == nullptr) {
        LOGE("aacEncodeChannel is null!");
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    jint size = env->GetArrayLength(data_);
    aacEncodeChannel->encodeData(reinterpret_cast<int32_t *>(data), size);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1stop(JNIEnv *env, jobject thiz) {
    if (!isStart) {
        return;
    }
    isStart = false;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1release(JNIEnv *env, jobject thiz) {
    packets.clear();
    if (opusEncodeChannel != nullptr) {
        delete opusEncodeChannel;
        opusEncodeChannel = nullptr;
    }
    if (opusDecodeChannel != nullptr) {
        delete opusDecodeChannel;
        opusDecodeChannel = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1setOpusDecInfo(JNIEnv *env, jobject thiz,
                                                                  jint sample_rate,
                                                                  jint channel_count) {
    if (opusDecodeChannel != nullptr) {
        opusDecodeChannel->setDecodeInfo(sample_rate, channel_count);
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_alexander_x264opusrtmp_LivePusher_native_1OpusDecode(JNIEnv *env, jobject thiz,
                                                              jbyteArray encoded_,
                                                              jshortArray decoded_) {
    jbyte *encoded = env->GetByteArrayElements(encoded_, nullptr);
    jshort *decoded = env->GetShortArrayElements(decoded_, nullptr);
    jint encodedSize = env->GetArrayLength(encoded_);
    jint decodedSize = env->GetArrayLength(decoded_);
    if (!(encodedSize > 0 && decodedSize > 0)) {
        LOGE("encoded or decoded data error!");
        return -1;
    }
    int ret = opusDecodeChannel->decodeData(reinterpret_cast<unsigned char *>(encoded),
                                            encodedSize, decoded, decodedSize);
    env->ReleaseByteArrayElements(encoded_, encoded, 0);
    env->ReleaseShortArrayElements(decoded_, decoded, 0);
    return ret;
}