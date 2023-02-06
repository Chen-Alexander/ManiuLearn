package com.maniu.weblib.command;

import android.content.Context;


import com.maniu.weblib.WebConstants;
import com.maniu.weblib.interfaces.ICommand;
import com.maniu.weblib.interfaces.ResultBack;

import java.util.Map;


public class BaseLevelACommands extends ACommands {

    public BaseLevelACommands() {
        registerCommands();
    }

    @Override
    int getCommandLevel() {
        return WebConstants.LEVEL_BASE;
    }

    void registerCommands() {
        registerCommand(pageRouterICommand);
    }

    /**
     * 页面路由
     */
    private final ICommand pageRouterICommand = new ICommand() {
        @Override
        public String name() {
            return "newPage";
        }

        @Override
        public void exec(Context context, Map params, ResultBack resultBack) {
            String newUrl = params.get("url").toString();
            String title = (String) params.get("title");
        }
    };
}
