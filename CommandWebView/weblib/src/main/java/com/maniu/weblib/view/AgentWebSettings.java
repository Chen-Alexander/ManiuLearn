package com.maniu.weblib.view;

import android.webkit.WebView;

public interface AgentWebSettings<T extends android.webkit.WebSettings>{

    AgentWebSettings toSetting(WebView webView);

    T getWebSettings();
}
