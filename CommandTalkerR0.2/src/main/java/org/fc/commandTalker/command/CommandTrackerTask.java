package org.fc.commandTalker.command;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.fc.commandTalker.App;

import java.util.Map;

public class CommandTrackerTask extends BukkitRunnable {
    public final CommandTrackerListener.CommandData commandData;
    private final Player player;
    public CommandTrackerTask(CommandTrackerListener.CommandData commandData, Player player) {
        this.commandData = commandData;
        this.player = player;
    }
    @Override
    public void run() {
        Map<String, Integer> commandUsageCount = commandData.getCommandUsageCount();
        int totalCommands = commandUsageCount.values().stream().mapToInt(Integer::intValue).sum();
        int uniqueCommands = commandUsageCount.size();
        int level = App.getCommandLevel(player);
        //System.out.println("CommandCount"+totalCommands);
        // スコアボードを作成
        ScoreboardManager manager = player.getServer().getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        // スコアボードのオブジェクトを再取得
        Objective objective = player.getScoreboard().getObjective("commandStats");
        if (scoreboard!= null && objective != null) {
            // 更新されるスコアの取得 (最初はすべて0)
            Score usedScore = objective.getScore("Commands Used:");
            Score uniqueScore = objective.getScore("Unique Commands:");
            Score levelScore = objective.getScore("Command level:");
            if(App.locale.contains("jp")){
                usedScore = objective.getScore("コマンド使用回数:");
                uniqueScore = objective.getScore("使ったコマンドの種類数:");
                levelScore = objective.getScore("コマンドレベル:");
            }
            // スコアの値を更新 (例として、それぞれ+0で初期値維持)
            usedScore.setScore(1);   // 初期スコアを0
            uniqueScore.setScore(0); // 初期スコアを0
            levelScore.setScore(-1);  // 初期スコアを0

            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);  // 全エントリを削除
            }
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);  // サイドバー表示をクリア

            // スコアの値を更新 (例として、それぞれ+0で初期値維持)
            usedScore.setScore(totalCommands);   // 初期スコアを0
            uniqueScore.setScore(uniqueCommands); // 初期スコアを0
            levelScore.setScore(level);  // 初期スコアを0
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

    }
}




