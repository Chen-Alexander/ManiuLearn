package com.maniu.weblib.command;

import android.content.Context;


import com.maniu.weblib.WebConstants;
import com.maniu.weblib.interfaces.AidlError;
import com.maniu.weblib.interfaces.ICommand;
import com.maniu.weblib.interfaces.ResultBack;

import java.util.Map;


public class CommandsManager {

    private static CommandsManager instance;

    private UIDependencyACommands uiDependencyCommands;
    private BaseLevelACommands baseLevelCommands;
    private AccountLevelACommands accountLevelCommands;

    private CommandsManager() {
        uiDependencyCommands = new UIDependencyACommands();
        baseLevelCommands = new BaseLevelACommands();
        accountLevelCommands = new AccountLevelACommands();
    }

    public static CommandsManager getInstance() {
        if (instance == null) {
            synchronized (CommandsManager.class) {
                instance = new CommandsManager();
            }
        }
        return instance;
    }

    /**
     * 动态注册command
     * 应用场景：其他模块在使用WebView的时候，需要增加特定的command，动态加进来
     */
    public void registerCommand(int commandLevel, ICommand ICommand) {
        switch (commandLevel) {
            case WebConstants.LEVEL_UI:
                uiDependencyCommands.registerCommand(ICommand);
                break;
            case WebConstants.LEVEL_BASE:
                baseLevelCommands.registerCommand(ICommand);
                break;
            case WebConstants.LEVEL_ACCOUNT:
                accountLevelCommands.registerCommand(ICommand);
                break;
        }
    }


    /**
     * 非UI Command 的执行
     */
    public void findAndExecNonUICommand(Context context, int level, String action, Map params, ResultBack resultBack) {
        boolean methodFlag = false;
        switch (level) {
            case WebConstants.LEVEL_BASE: {
                if (baseLevelCommands.getCommands().get(action) != null) {
                    methodFlag = true;
                    baseLevelCommands.getCommands().get(action).exec(context, params, resultBack);
                }
                if (accountLevelCommands.getCommands().get(action) != null) {
                    AidlError aidlError = new AidlError(WebConstants.ERRORCODE.NO_AUTH, WebConstants.ERRORMESSAGE.NO_AUTH);
                    resultBack.onResult(WebConstants.FAILED, action, aidlError);
                }
                break;
            }
            case WebConstants.LEVEL_ACCOUNT: {
                if (baseLevelCommands.getCommands().get(action) != null) {
                    methodFlag = true;
                    baseLevelCommands.getCommands().get(action).exec(context, params, resultBack);
                }
                if (accountLevelCommands.getCommands().get(action) != null) {
                    methodFlag = true;
                    accountLevelCommands.getCommands().get(action).exec(context, params, resultBack);
                }
                break;
            }
        }
        if (!methodFlag) {
            AidlError aidlError = new AidlError(WebConstants.ERRORCODE.NO_METHOD, WebConstants.ERRORMESSAGE.NO_METHOD);
            resultBack.onResult(WebConstants.FAILED, action, aidlError);
        }
    }

    /**
     * UI  Command的执行
     */
    public void findAndExecUICommnad(Context context, int level, String action, Map params, ResultBack resultBack) {
        if (uiDependencyCommands.getCommands().get(action) != null) {
            ICommand command = uiDependencyCommands.getCommands().get(action);
            command.exec(context, params, resultBack);
        }
    }

    public boolean checkHitUICommand(int level, String action) {
        return uiDependencyCommands.getCommands().get(action) != null;
    }

}

