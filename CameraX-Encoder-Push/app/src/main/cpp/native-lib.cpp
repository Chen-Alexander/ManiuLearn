#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>

#include "libyuv/include/libyuv.h"
#include "libyuv/include/libyuv/rotate.h"

#define LOG_TAG "libyuv"
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace libyuv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraxencoderpush_YuvUtils_NV21ToI420(JNIEnv *env, jclass instance, jbyteArray input_,
                                             jbyteArray output_, jint in_width, jint in_height) {
    jbyte *srcData = env->GetByteArrayElements(input_, NULL);
    jbyte *dstData = env->GetByteArrayElements(output_, NULL);

    NV21ToI420(
            (const uint8_t *) srcData,
            in_width,
            (const uint8_t *) srcData + in_width * in_height,
            in_width,
            (uint8_t *) dstData,
            in_width,
            (uint8_t *) dstData + in_width * in_height,
            (in_width + 1) / 2,
            (uint8_t *) dstData + in_width * in_height + ((in_width + 1) / 2) * ((in_height + 1) / 2),
            (in_width + 1) / 2,
            in_width,
            in_height);

    env->ReleaseByteArrayElements(input_, srcData, 0);
    env->ReleaseByteArrayElements(output_, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraxencoderpush_YuvUtils_RotateI420(JNIEnv *env, jclass type, jbyteArray input_,
                                             jbyteArray output_, jint in_width, jint in_height,
                                             jint rotation) {
    jbyte *srcData = env->GetByteArrayElements(input_, NULL);
    jbyte *dstData = env->GetByteArrayElements(output_, NULL);


    RotationMode rotationMode;
    switch (rotation) {
        case 90:
            rotationMode = kRotate90;
            break;
        case 180:
            rotationMode = kRotate180;
            break;
        case 270:
            rotationMode = kRotate270;
            break;
        default:
            rotationMode = kRotate0;
            break;
    }
    int dst_width = in_width;
    int dst_height = in_height;
    I420Rotate(
            /*????????????y??????????????????*/
            (const uint8_t *) srcData,
            /*????????????y???????????????????????????*/
            in_width,
            /*????????????u????????????????????? */
            (const uint8_t *) srcData + in_width * in_height,
            /*????????????u???????????????????????????*/
            (in_width + 1) / 2,
            /*????????????v????????????????????? */
            (const uint8_t *) srcData + in_width * in_height + ((in_width + 1) / 2) * ((in_height + 1) / 2),
            /*????????????v???????????????????????????*/
            (in_width + 1) / 2,
            /*???????????????y??????????????????*/
            (uint8_t *) dstData,
            /*???????????????y???????????????????????????*/
            in_height,
            /*???????????????u??????????????????*/
            (uint8_t *) dstData + in_width * in_height,
            /*???????????????u???????????????????????????*/
            (in_height + 1) / 2,
            /*???????????????v??????????????????*/
            (uint8_t *) dstData + in_width * in_height + ((in_width + 1) / 2) * ((in_height + 1) / 2),
            /*???????????????v???????????????????????????*/
            (in_height + 1) / 2,
            dst_width,
            dst_height,
            /*????????????*/
            rotationMode);

    env->ReleaseByteArrayElements(input_, srcData, 0);
    env->ReleaseByteArrayElements(output_, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraxencoderpush_YuvUtils_Flip(JNIEnv *env, jclass clazz, jbyteArray input,
                                       jbyteArray output, jint width, jint height) {
    jbyte *srcData = env->GetByteArrayElements(input, NULL);
    jbyte *dstData = env->GetByteArrayElements(output, NULL);

    I420Mirror(
            (const uint8_t *) srcData,
            width,
            (const uint8_t *) srcData + width * height,
            (width + 1) / 2,
            (const uint8_t *) srcData + width * height + ((width + 1) / 2) * ((height + 1) / 2),
            (width + 1) / 2,
            (uint8_t *) dstData,
            width,
            (uint8_t *) dstData + width * height,
            (width + 1) / 2,
            (uint8_t *) dstData + width * height + ((width + 1) / 2) * ((height + 1) / 2),
            (width + 1) / 2,
            width,
            height);

    env->ReleaseByteArrayElements(input, srcData, 0);
    env->ReleaseByteArrayElements(output, dstData, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraxencoderpush_YuvUtils_NV21ToI420andRotate90Clockwise(JNIEnv *env, jclass type,
                                                                 jbyteArray input_,
                                                                 jbyteArray output_, jint in_width,
                                                                 jint in_height) {
    jbyte *srcData = env->GetByteArrayElements(input_, NULL);
    jbyte *dstData = env->GetByteArrayElements(output_, NULL);
    jsize size = env->GetArrayLength(input_);

    NV21ToI420((const uint8_t *) srcData, in_width,
               (uint8_t *) srcData + (in_width * in_height), in_width,
               (uint8_t *) dstData, in_width,
               (uint8_t *) dstData + (in_width * in_height), in_width / 2,
               (uint8_t *) dstData + (in_width * in_height * 5 / 4), in_width / 2,
               in_width, in_height);

    I420Rotate((const uint8_t *) dstData, in_width,
               (uint8_t *) dstData + (in_width * in_height), in_width / 2,
               (uint8_t *) dstData + (in_width * in_height * 5 / 4), in_width / 2,
               (uint8_t *) srcData, in_height,
               (uint8_t *) srcData + (in_width * in_height), in_height / 2,
               (uint8_t *) srcData + (in_width * in_height * 5 / 4), in_height / 2,
               in_width, in_height, kRotate90);

    memcpy(dstData, srcData, size);

//    fixme can't work
//    ConvertToI420((const uint8_t *) srcData, size,
//                  (uint8_t *)dstData, in_width,
//                  (uint8_t *)dstData + (in_width * in_height), in_width / 2,
//                  (uint8_t *)dstData + (in_width * in_height * 5 / 4), in_width / 2,
//                  0, 0,
//                  in_width, in_height,
//                  in_width, in_height,
//                  kRotate90,
//                  FOURCC_NV21);
//
//   fixme can't work
//    NV12ToI420Rotate((const uint8_t *) srcData, in_width,
//                     (uint8_t *) srcData + (in_width * in_height), in_width,
//                     (uint8_t *)dstData, in_width,
//                     (uint8_t *)dstData + (in_width * in_height * 5 / 4), in_width / 2,
//                     (uint8_t *)dstData + (in_width * in_height), in_width / 2,
//                     in_width, in_height,
//                     kRotate90);

    env->ReleaseByteArrayElements(input_, srcData, 0);
    env->ReleaseByteArrayElements(output_, dstData, 0);
}