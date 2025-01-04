package org.fc.commandTalker.command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.fc.commandTalker.App;

import java.util.HashMap;
import java.util.Map;
public class CommandTrackerListener implements Listener {
    private final CommandData commandData;
    public CommandTrackerListener(CommandData commandData) {
        this.commandData = commandData;
    }
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        if(msg.equals("/")) {
            //System.out.println("No Count");
            return;
        }
        String command[] = msg.split(" "); // コマンドの最初のワードを取得
        commandData.countCommand(command[0]);
    }
    public CommandData getCommandData() {
        if(commandData == null){
            return null;
        }
        return commandData;
    }
    public static class CommandData {
        private final Map<String, Integer> commandUsageCount = new HashMap<>();
        // コマンドをカウント
        public void countCommand(String command) {
            commandUsageCount.put(command, commandUsageCount.getOrDefault(command, 0) + 1);
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
}
