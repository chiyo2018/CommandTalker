package org.fc.commandTalker.command;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Map;

public class CommandDataLoader {
    private final JavaPlugin plugin;
    public CommandDataLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    // コマンドデータをファイルに書き込む
    public void saveCommandData(CommandTrackerListener.CommandData data) {
        File file = new File(plugin.getDataFolder(), "command_data.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            Map<String, Integer> commandUsageCount = data.getCommandUsageCount();
            for (Map.Entry<String, Integer> entry : commandUsageCount.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // コマンドデータをファイルから読み込む
    public CommandTrackerListener.CommandData loadCommandData() {
        File file = new File(plugin.getDataFolder(), "command_data.txt");
        CommandTrackerListener.CommandData data = new CommandTrackerListener.CommandData();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String command = parts[0];
                        int count = Integer.parseInt(parts[1]);
                        for (int i = 0; i < count; i++) {
                            data.countCommand(command);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}
