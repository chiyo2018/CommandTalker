package org.fc.commandTalker.command;


import java.util.HashMap;
import java.util.Map;

public class CommandData {
    private final Map<String, Integer> commandUsageCount = new HashMap<>();
    // コマンドをカウント
    public void countCommand(String command) {
        if(!command.equals("/")){
            commandUsageCount.put(command, commandUsageCount.getOrDefault(command, 0) + 1);
        }
    }
    // コマンドの種類数
    public int getUniqueCommandCount() {
        return commandUsageCount.size();
    }
    // コマンドの使用回数
    public int getTotalCommandCount() {
        return commandUsageCount.values().stream().mapToInt(Integer::intValue).sum();
    }
    // コマンド使用データの取得
    public Map<String, Integer> getCommandUsageCount() {
        return new HashMap<>(commandUsageCount);
    }
}


