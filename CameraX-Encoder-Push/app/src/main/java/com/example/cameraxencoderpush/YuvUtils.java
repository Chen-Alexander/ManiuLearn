package com.example.cameraxencoderpush;

public class YuvUtils {

    static {
        System.loadLibrary("native-lib");
    }

    // 创建一个YuvUtils，有三个常用的方法，第一个就是NV21转I420，然后旋转I420，最后一个是NV21转换I420并顺时针旋转90度，可以替换前两个方法
    public static native void NV21ToI420(byte[] input, byte[] output, int width, int height);
    public static native void RotateI420(byte[] input, byte[] output, int width, int height, int rotation);
    public static native void Flip(byte[] input, byte[] output, int width, int height);
}
