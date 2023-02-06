package com.maniu.weblib.interfaces;

import android.content.Context;

import java.util.Map;

public interface ICommand {

    String name();
    //执行的意思
    void exec(Context context, Map params, ResultBack resultBack);
}
