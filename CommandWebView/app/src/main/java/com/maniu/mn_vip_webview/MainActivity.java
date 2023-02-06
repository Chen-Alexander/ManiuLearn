package com.maniu.mn_vip_webview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.maniu.weblib.WebConstants;
import com.maniu.weblib.view.DWebView;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("MainActivity", "onCreate on " + Process.myPid() + "->" + Thread.currentThread().getName());

        findViewById(R.id.openWeb1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebActivity.start(MainActivity.this, "腾讯网",
                        "https://xw.qq.com/?f=qqcom", WebConstants.LEVEL_BASE);
            }
        });

        findViewById(R.id.openWeb2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // for account level
                WebActivity.start(MainActivity.this,
                        "AIDL测试", DWebView.CONTENT_SCHEME + "aidl.html", WebConstants.LEVEL_ACCOUNT);
            }
        });
    }

    private void testWebViewInitUsedTime() {
        long p = System.currentTimeMillis();
        WebView mWebView = new WebView(this);
        long n = System.currentTimeMillis();
        Log.e("MN--------->", "testWebViewFirstInit use time:" + (n - p));
    }

    public void webViewInit(View view) {
        testWebViewInitUsedTime();
        testWebViewInitUsedTime();

        RelativeLayout relativeLayout = findViewById(R.id.webViewWrap);
        BaseApplication.webView.loadUrl("https://www.baidu.com/");
        relativeLayout.removeAllViews();
        relativeLayout.addView(BaseApplication.webView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BaseApplication.webView.clearCache(true);//清除缓存
    }


    /**
     * 使用反射的方式来清楚WebView的缓存
     *
     * @param windowManager
     */
    public void setConfigCallback(WindowManager windowManager) {
        try {
            Field field = WebView.class.getDeclaredField("mWebViewCore");
            field = field.getType().getDeclaredField("mBrowserFrame");
            field = field.getType().getDeclaredField("sConfigCallback");
            field.setAccessible(true);
            Object configCallback = field.get(null);
            if (null == configCallback) {
                return;
            }
            field = field.getType().getDeclaredField("mWindowManager");
            field.setAccessible(true);
            field.set(configCallback, windowManager);
        } catch (Exception e) {
        }

    }
}