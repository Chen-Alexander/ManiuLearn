package com.maniu.bitmapmaniu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.app.Person;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    int[] arr;
    Bitmap bitmap;
    ImageView image;
    ImageHandler gifHandler;
    ImageHandler imageHandler = null;
    PlayGifTask mGifTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        image= (ImageView) findViewById(R.id.image);

    }
    public   void verifyStoragePermissions(Activity activity) {
        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE" };
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//glide
//    加载速度
//    内存开销
//    glide      gif 版本   使用gif文件  兼容  bitmap成果而已
    public void loadBitmap(View view) {
        File file=new File(Environment.getExternalStorageDirectory(),"demo.gif");
        gifHandler = ImageHandler.load(file.getAbsolutePath());
        int width=gifHandler.getWidth();
        int height=gifHandler.getHeight();
        bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        int delay=   gifHandler.updateFrame(bitmap);
        image.setImageBitmap(bitmap);

        myHandler.sendEmptyMessageDelayed(1, delay);
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            int delay=gifHandler.updateFrame(bitmap);
            myHandler.sendEmptyMessageDelayed(1,delay);
            image.setImageBitmap(bitmap);
        }
    };




    public void javaLoadGif(View view) {
        //对Gif图片进行解码
        InputStream fis =null;

        try {
            fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(),"demo.gif"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        GifHelper gifHelper=new GifHelper();
        gifHelper.read(fis);
        mGifTask = new PlayGifTask(image, gifHelper.getFrames());
        mGifTask.startTask();
        Thread th=new Thread(mGifTask);
        th.start();
    }
    //用来循环播放Gif每帧图片
    private class PlayGifTask implements Runnable {
        int i = 0;
        ImageView iv;
        GifHelper.GifFrame[] frames;
        int framelen,oncePlayTime=0;

        public PlayGifTask(ImageView iv, GifHelper.GifFrame[] frames) {
            this.iv = iv;
            this.frames = frames;

            int n=0;
            framelen=frames.length;
            while(n<framelen){
                oncePlayTime+=frames[n].delay;
                n++;
            }
            Log.d("msg", "playTime= "+oncePlayTime);

        }

        Handler h2=new Handler(){
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case 1:
                        iv.setImageBitmap((Bitmap)msg.obj);
                        break;
                }
            };
        };
        @Override
        public void run() {
            if (!frames[i].image.isRecycled()) {
                //      iv.setImageBitmap(frames[i].image);
                Message m= Message.obtain(h2, 1, frames[i].image);
                m.sendToTarget();
            }
            iv.postDelayed(this, frames[i++].delay);
            i %= framelen;
        }

        public void startTask() {
            iv.post(this);
        }

        public void stopTask() {
            if(null != iv) iv.removeCallbacks(this);
            iv = null;
            if(null != frames) {
                for(GifHelper.GifFrame frame : frames) {
                    if(frame.image != null && !frame.image.isRecycled()) {
                        frame.image.recycle();
                        frame.image = null;
                    }
                }
                frames = null;
            }
        }
    }
}