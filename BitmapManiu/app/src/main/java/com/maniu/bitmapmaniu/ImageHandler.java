package com.maniu.bitmapmaniu;

import android.graphics.Bitmap;

public class ImageHandler {
    long gifHander = 0;
    static {
        System.loadLibrary("bitmapmaniu");
    }
    public static native long loadGif(String path);

    private ImageHandler(long gifHander) {
        this.gifHander = gifHander;
    }
    public static ImageHandler load(String path) {
        long gifHander=loadGif(path);
        ImageHandler imageHandler = new ImageHandler(gifHander);
        return imageHandler;
    }

    public  int getWidth(){
        return getWidth(gifHander);
    }
    public  int getHeight(){
        return getHeight(gifHander);
    }



    //    bitmap   ----gif
//gifHander  地址 不了解     Bitmap   思想      long  nativePtr
    public static native int getWidth(long gifHander);
    public static native int getHeight(long gifPoint);

    public   int   updateFrame(Bitmap bitmap ){
        return updateFrame(gifHander,bitmap);
    }

    public native int updateFrame(long gifPoint,Bitmap bitmap);
}
