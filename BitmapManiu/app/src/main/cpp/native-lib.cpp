#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#define  LOG_TAG    "David"
#define  argb(a,r,g,b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
extern "C"{
#include "gif_lib.h"
}
struct GifBean{
    int current_frame;
    int total_frame;
};
//extern "C"
//JNIEXPORT jint JNICALL
//Java_com_maniu_bitmapmaniu_ImageHandler_updateFrame(JNIEnv *env, jobject thiz,
//                                                    jobject bitmap) {
//    AndroidBitmapInfo info;
////     函数  bitmap
//    int *pixels=NULL;
//    AndroidBitmap_getInfo(env, bitmap, &info);
//    int width = info.width;
//    int height = info.height;
//    LOGE("宽%d  高%d:  ",width,height);
//    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixels));
////    pixels[0] = 0xFF0000; gif
////c  数组
//    int* px =  pixels;
//    int *line;
//    for (int y = 0; y < height;  y++) {
//        line = px;
//        for (int x = 0; x < width;  x++) {
////            红色 ？
////程序上运行 argb  a b g  r
//            line[x] = 0xFF00FF00;
//        }
////        argb r g
//        px = px + width;
//    }
//    AndroidBitmap_unlockPixels(env, bitmap);
//    return 1;
//}
//初始化
#define  argb(a,r,g,b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_maniu_bitmapmaniu_ImageHandler_loadGif(JNIEnv *env, jclass clazz, jstring path_) {
//对gif解码
    const char *path = env->GetStringUTFChars(path_, 0);
    int Erro;//打开失败还是成功 0  1  解码  数据分类   xml  文件    解析工具     javabean   GifFileType
    GifFileType *gifFileType =  DGifOpenFileName(path, &Erro);
//    初始化
    DGifSlurp(gifFileType);
//    使用   项目不需要写gif    gif   ---》 glide  gif 性能  glide java
    GifBean *gifBean = static_cast<GifBean *>(malloc(sizeof(GifBean)));
//    清空
    memset(gifBean, 0, sizeof(GifBean));
    LOGE("宽%d  高%d:  ",gifFileType->SWidth,gifFileType->SHeight);
//    赋值
    gifBean->total_frame = gifFileType->ImageCount;
    gifBean->current_frame = 0;
    gifFileType->UserData = gifBean;
    env->ReleaseStringUTFChars(path_, path);
    return reinterpret_cast<jlong>(gifFileType);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_maniu_bitmapmaniu_ImageHandler_getWidth(JNIEnv *env, jclass clazz, jlong gif_hander) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_hander);
    return gifFileType->SWidth;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_maniu_bitmapmaniu_ImageHandler_getHeight(JNIEnv *env, jclass clazz, jlong gif_point) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_point);
    return gifFileType->SHeight;
}
void drawFrame1(GifFileType* gifFileType, AndroidBitmapInfo info, void *pixels) {
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    gifBean->current_frame;
//    取出待绘制的第几帧
    SavedImage savedImage = gifFileType->SavedImages[gifBean->current_frame];

//    savedImage数据 怎么 最大难题

    GifImageDesc frameInfo=  savedImage.ImageDesc;
//    颜色表
    //   数组下标 索引的
    int  pointPixel;
//颜色表的索引
    GifByteType  gifByteType;
    //颜色
    GifColorType gifColorType;
    ColorMapObject *colorMapObject = frameInfo.ColorMap;
    int* px = (int *)pixels;
    int *line;
    for (int y =0 ; y<frameInfo.Height; ++y) {
        line = px;
        for (int x =  0; x < frameInfo.Width ; ++x) {
//            里面写出   数据
            pointPixel = (y ) * frameInfo.Width + (x);
            gifByteType=  savedImage.RasterBits[pointPixel];
            gifColorType= colorMapObject->Colors[gifByteType];
            line[x] = argb(255, gifColorType.Red, gifColorType.Green, gifColorType.Blue);
        }
        px = px + info.stride / 4;
    }


}
extern "C"
JNIEXPORT jint JNICALL
Java_com_maniu_bitmapmaniu_ImageHandler_updateFrame(JNIEnv *env, jobject thiz,
                                                    jlong gif_point,
                                                    jobject bitmap) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_point);

    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    gifBean->current_frame++;
    if (gifBean->current_frame >= gifBean->total_frame - 1) {
        gifBean->current_frame = 0;
    }
//    控制操作

//绘制操作
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    int width = info.width;
    int height = info.height;
    LOGE("宽%d  高%d:  ",width,height);
    int *pixels=NULL;
//2   我们要拿到native层的那个指针
    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixels));
//        pixels  hua
    drawFrame1(gifFileType, info, pixels);
    AndroidBitmap_unlockPixels(env, bitmap);
    return 100;

//延迟时间   严

}