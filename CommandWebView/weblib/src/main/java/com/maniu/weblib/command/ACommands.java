package com.maniu.weblib.command;


import com.maniu.weblib.interfaces.ICommand;

import java.util.HashMap;

/**
 * 某一种命令类型下所有命令对象的基类
 * 比如  我要处理的命令有哪些
 */
public abstract class ACommands {
    //是对每一种命令的集合
    private HashMap<String, ICommand> commands;

    abstract int getCommandLevel();

    public HashMap<String, ICommand> getCommands() {
        return commands;
    }

    public ACommands() {
        commands = new HashMap<>();
    }

    protected void registerCommand(ICommand ICommand) {
        commands.put(ICommand.name(), ICommand);
    }
}


