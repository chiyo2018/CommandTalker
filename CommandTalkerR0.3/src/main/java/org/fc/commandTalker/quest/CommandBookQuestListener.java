package org.fc.commandTalker.quest;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.fc.commandTalker.App;
import org.fc.commandTalker.MyGPT;

import java.util.*;

import static org.apache.logging.log4j.LogManager.getLogger;

public class CommandBookQuestListener implements Listener {
    private final Map<Player, Boolean> awaitingResponse = new HashMap<>();
    private JavaPlugin plugin;
    private MyGPT myGPT;
    private Player player;
    private static final String COMMANDER_SWORD_NAME = "§6Commander Sword";
    public CommandBookQuestListener(JavaPlugin plugin) {
        this.plugin = plugin;
        myGPT = new MyGPT(this.plugin);
    }
    // プレイヤーのスコアボードから特定のスコア値を取得する関数
    public static int getTextForScore(Player player, String str) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("commandStats");
        if (objective != null) {
            // "Unique Commands:" の現在のスコアを取得
            Score uniqueScore = objective.getScore(str);
            return uniqueScore.getScore();  // スコア値を返す
        }
        return 0;  // スコアが取得できない場合のデフォルト値
    }
    @EventHandler
    public void onPlayerUseBook(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        int level = App.getCommandLevel(player);
        if( level ==5 ){
            getLogger().info( "コマンドブックは完成しました！");
            return;
        }
        int status;
        if(!player.getMetadata("QuestStat").isEmpty()){
            status = player.getMetadata("QuestStat").getFirst().asInt();
        }else status = 0;
        String questMsg =  "クエストを受けますか？ (yes/no)";
        if(status == 1){
            questMsg =  "クエストをリセットしますか？ (yes/no)";
        }
        if (item != null && item.getType() == Material.WRITTEN_BOOK) {
            BookMeta meta = (BookMeta) item.getItemMeta();
            if (meta != null && Objects.equals(meta.getTitle(), "CommandBook")) {
                myGPT.transGPT(player, questMsg);
                awaitingResponse.put(player, true);
            }
        }
    }
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        if (awaitingResponse.containsKey(player)) {
            if (message.equals("yes")) {
                player.setMetadata("QuestStat", new FixedMetadataValue(plugin, 1));
                player.setMetadata("QuestStat4", new FixedMetadataValue(plugin, 0));
                getLogger().info(player.getMetadata("QuestStat").getFirst().asInt());
                myGPT.transGPT(player,"クエスト開始！");
                //コマンドレベル別に処理
                ItemStack commandBook = getCommandBookFromInventory(player);
                BookMeta meta = (BookMeta) Objects.requireNonNull(commandBook).getItemMeta();
                int count = meta.getPageCount();
                if(count == 3) startQuest1(player);
                else if(count == 4) startQuest2(player);
                else if(count == 5) startQuest3(player);
                else if(count == 7) {
                    killAllZombies();
                    player.setMetadata("QuestStat4", new FixedMetadataValue(plugin,0));
                    startQuest4(player);
                }
            } else if (message.equals("no")) {
                myGPT.transGPT(player,"キャンセル");
            } else {
                myGPT.transGPT(player,"「yes」または「no」と入力してください");
                return;
            }
            awaitingResponse.remove(player);
        }
    }
    private void startQuest4(Player player) {
        myGPT.transGPT(player,"コマンドで魔王を呼び出しましょう。コマンドブックをよくみてください。");
        int totalCount = 0;
        if(App.locale.contains("jp")){
            totalCount = getTextForScore(player, "コマンド使用回数:");
        }else{
            totalCount = getTextForScore(player,"Unique Commands:");
        }
        myGPT.transGPT(player,"コマンド使用回数：" + totalCount);
        if(totalCount > 100 ){
            if(!hasCommanderSword(player)) {
                giveCommanderSword(player);
                myGPT.transGPT(player,"最強のコマンダーソードを手に入れました！");
            }
        }else{
            myGPT.transGPT(player,"もう少しコマンドを使うと、いいことがあるかも？");
        }
    }
    public static boolean hasCommanderSword(Player player) {
        // プレイヤーのインベントリを取得
        ItemStack[] inventory = player.getInventory().getContents();
        // インベントリの全アイテムをチェック
        for (ItemStack item : inventory) {
            if (item != null && item.hasItemMeta() && COMMANDER_SWORD_NAME.equals(item.getItemMeta().getDisplayName())) {
                return true; // アイテムが見つかった
            }
        }
        // 見つからない場合は false
        return false;
    }
    //最強の剣
    public static ItemStack createCommanderSword() {
        // ネザライトソードを作成
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            // 「コマンダーソード」の名前を設定
            meta.setDisplayName(COMMANDER_SWORD_NAME);
            // エンチャントを追加
            meta.addEnchant(Enchantment.SHARPNESS, 5, true); // 最大レベルのシャープネス
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true); // ファイアアスペクト2
            meta.addEnchant(Enchantment.UNBREAKING, 3, true); // 最大レベルの耐久力
            meta.addEnchant(Enchantment.MENDING, 1, true); // 修理
            meta.addEnchant(Enchantment.KNOCKBACK, 2, true); // ノックバック2
            // アイテムにメタデータを設定
            sword.setItemMeta(meta);
        }
        return sword;
    }
    public static void giveCommanderSword(Player player) {
        // プレイヤーに「コマンダーソード」を渡す
        ItemStack commanderSword = createCommanderSword();
        player.getInventory().addItem(commanderSword);
    }
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        Player player = event.getPlayer();
        int level =App.getCommandLevel(player);
        // 許可されていないコマンドが実行されようとしている場合、キャンセルする
        if (!isCommandAllowed(command, level)) {
            myGPT.transGPT(player, "このコマンドはまだ使用できません。現在のコマンダーレベル＝" + level);
            event.setCancelled(true);
        }else if (command.equals("/execute as @e run say CommandKing")) {
            // ワールド内のすべてのモブに発話させ
            makeAllMobsSpeak("CommandKing");
            World world = player.getWorld();
            world.setTime(13000);
            world.setStorm(true);
            PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 10, 0);
            player.addPotionEffect(nightVision);
            // プレイヤーをテレポート
            createFloorAndTeleport(player);
            // プレイヤーの前にジャイアントゾンビをスポーン
            spawnGiantZombie(player);
            event.setCancelled(true); // コマンドを実行しないようにキャンセル
        }
    }
    // コマンドが許可されているかを確認するメソッド
    private boolean isCommandAllowed(String command, int commandLevel) {
        if(commandLevel < 5 ){
            return switch (command.split(" ")[0]) {
                case "/time", "/weather" -> true; // 最初から許可されているコマンド
                case "/tp" -> commandLevel >= 2; // コマンドレベルが2以上なら許可
                case "/locate" -> commandLevel >= 3; // コマンドレベルが3以上なら許可
                case "/execute", "/say" -> commandLevel >= 4;// コマンドレベルが4以上なら許可
                default -> false; // すべて禁止
            };
        }else{
            return true;//すべて許可
        }
    }
    // ワールド内のすべてのモブに発話させる関数
    public void makeAllMobsSpeak(String message) {
        // サーバー上のすべてのワールドをループ
        Bukkit.getWorlds().forEach(world -> {
            // ワールド内のすべてのエンティティを取得
            for (Entity entity : world.getEntities()) {
                // LivingEntity（生物）だけを対象にする
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    // 発話する（チャットメッセージを送信）
                    livingEntity.sendMessage(message);
                }
            }
        });
    }
    // プレイヤーの上空100ブロックに50x50の床を作り、プレイヤーをその床の中心にテレポート
    public void createFloorAndTeleport(Player player) {
        // プレイヤーの現在位置を取得
        double playerX = player.getLocation().getX();
        double playerY = player.getLocation().getY();
        double playerZ = player.getLocation().getZ();
        // 上空50ブロックの位置を計算
        int floorY = (int) playerY + 100;
        // 50x50の床を作成
        for (int x = -25; x < 25; x++) {
            for (int z = -25; z < 25; z++) {
                Block block = player.getWorld().getBlockAt( 0, 200, 0);
                block.setType(Material.STONE); // 床を作るためのブロック（石ブロック）
            }
        }
        // プレイヤーを床の中心にテレポート
        Location loc =new Location(player.getWorld(), 0,201,0);
        player.teleport(loc);
    }
    private void spawnGiantZombie(Player player) {
        // プレイヤーの目の前にジャイアントゾンビをスポーン
        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        Giant giant = player.getWorld().spawn(spawnLocation, Giant.class);
        // ジャイアントゾンビの周囲にゾンビを5匹スポーン
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            double angle = 2 * Math.PI * random.nextDouble();
            double radius = 3; // 半径3ブロックの範囲
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            Location zombieLocation = spawnLocation.clone().add(offsetX, 0, offsetZ);
            player.getWorld().spawn(zombieLocation, Zombie.class);
        }
        // ジャイアントゾンビが倒されたらクエストクリア
        giant.setCustomName("CommandKing Giant");
        giant.setCustomNameVisible(true);
        giant.setRemoveWhenFarAway(false);
    }
    // エンドロールを表示するメソッド
    private void startEndRoll(Player player) {
        // エンドロールテキスト
        List<String> endRollText = List.of(
                ChatColor.GOLD + "=== Minecraft End Roll ===",
                ChatColor.AQUA + "Thanks for playing!",
                ChatColor.GREEN + "Developed by C.Sawada",
                ChatColor.YELLOW + "Special Thanks to the Keio University",
                ChatColor.RED + "You can use ALl Commands! "
        );
        // 音楽再生
        player.playSound(player.getLocation(), Sound.MUSIC_DISC_OTHERSIDE, 1.0f, 1.0f);
        // エンドロール開始
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= endRollText.size()) {
                    this.cancel();
                    myGPT.transGPT(player,ChatColor.LIGHT_PURPLE + "エンドロールが終了しました。");
                    return;
                }
                // プレイヤーにタイトル表示
                player.sendTitle("", endRollText.get(index), 10, 70, 20);
                // パーティクル効果
                Location loc = player.getLocation().add(0, 2, 0);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 1, 1, 1, 0.1);
                index++;
            }
        }.runTaskTimer(plugin, 0, 100); // 100 ticks = 5秒間隔
    }
    private void startQuest3(Player player) {
        myGPT.transGPT(player,"森の洋館を探索してください！");
    }
    // 現在地が森の洋館かどうかを判定するメソッド
    public boolean isInWoodlandMansion(Location location) {
        World world = location.getWorld();
        // 森の洋館の構造が存在するかをチェック
        return world.hasStructureAt(location, Structure.MANSION);
    }
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer(); // プレイヤーを取得
        Inventory inventory = event.getInventory(); // 開かれたインベントリを取得
        // インベントリがチェストかどうかを確認
        if (inventory.getHolder() instanceof Chest) {
            Chest chest = (Chest) inventory.getHolder();
            Block chestBlock = chest.getBlock();
            Location chestLocation = chestBlock.getLocation();
            World world = chestLocation.getWorld();
            if (world != null && isInWoodlandMansion(chestLocation) &&
                    player.getMetadata("QuestStat4").get(0).asInt() == 0) {
                myGPT.transGPT(player,"§aあなたは森の洋館のチェストを開きました！");
                player.setMetadata("QuestStat4", new FixedMetadataValue(plugin, 1));
                completeQuest(player);
            }
        }
    }

    private void startQuest2(Player player) {
        myGPT.transGPT(player,"自分のまわりにウサギを10匹あつめてください。");
        spawnAnimalsNearPlayer(player);
        myGPT.transGPT(player,"大量のウサギと猫を生成しました。急いでウサギを集めてください。");
        checkRabbitsAroundPlayer(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isQuestComplete(player, player.getLocation())) {
                    completeQuest(player);
                    cancel(); // クエスト完了後、スケジュールをキャンセル
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 5); // 5秒ごとにチェック（20 ticks = 1秒）
    }
    private static final double CHECK_RADIUS = 5.0;    // チェックする半径
    public void checkRabbitsAroundPlayer(Player player) {
        // プレイヤーの周囲のエンティティを取得
        Location playerLocation = player.getLocation();
        int rabbitCount = 0;
        for (Entity entity : player.getNearbyEntities(CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS)) {
            if (entity.getType() == EntityType.RABBIT) {
                rabbitCount++;
            }
        }

    }
    // プレイヤー周辺50ブロック以内にうさぎ10匹と猫5匹をスポーンさせるメソッド
    public void spawnAnimalsNearPlayer(Player player) {
        Random random = new Random();
        Location playerLocation = player.getLocation();
        World world = playerLocation.getWorld();
        // うさぎをスポーン
        for (int i = 0; i < 30; i++) {
            double xOffset = random.nextDouble() * 100 - 30; // -50 から 50 の範囲
            double zOffset = random.nextDouble() * 100 - 30; // -50 から 50 の範囲
            Location spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            // うさぎをスポーン
            world.spawnEntity(spawnLocation, EntityType.RABBIT);
        }
        // 猫をスポーン
        for (int i = 0; i < 1; i++) {
            double xOffset = random.nextDouble() * 100 - 50; // -50 から 50 の範囲
            double zOffset = random.nextDouble() * 100 - 50; // -50 から 50 の範囲
            Location spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            // 猫をスポーン
            world.spawnEntity(spawnLocation, EntityType.CAT);
        }
        myGPT.transGPT(player,"あなたのまわりにウサギ30匹と猫10匹がスポーンしました!");
    }
    // うさぎ小屋に10匹のうさぎが入ったか確認
    public boolean isQuestComplete(Player player, Location playerLocation) {
        World world = playerLocation.getWorld();
        int rabbitCount = 0;
        // 周辺にいるエンティティを確認
        for (Entity entity : world.getNearbyEntities(playerLocation, 5, 5, 5)) {
            if (entity.getType() == EntityType.RABBIT) {
                rabbitCount++;
            }
        }
        // うさぎが10匹以上ならクエストクリア
        return rabbitCount >= 10;
    }
    private void startQuest1(Player player) {
        this.player = player;
        myGPT.transGPT(player,ChatColor.GREEN + "時間を夜に変更します！");
        player.setMetadata("QuestStat", new FixedMetadataValue(plugin, 1));
        Location playerLocation = player.getLocation();
        Random random = new Random();
        World world = player.getWorld();
        world.setTime(13000);
        world.setStorm(false);
        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 10, 0);
        player.addPotionEffect(nightVision);
        myGPT.transGPT(player,ChatColor.GREEN + "時間を夜に変更します！");
        myGPT.transGPT(player,ChatColor.GREEN + "天気を晴れに変更します！");
        myGPT.transGPT(player,ChatColor.GREEN + "プレイヤーに暗視効果を付与します！");
        random = new Random();
        for (int i = 0; i < 50; i++) {
            double xOffset, zOffset;
            Location spawnLocation;
            do {
                xOffset = random.nextDouble() * 100 - 50;
                zOffset = random.nextDouble() * 100 - 50;
                spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            } while (spawnLocation.distance(playerLocation) < 10);
            spawnLocation.setY(world.getHighestBlockYAt(spawnLocation));
            Zombie zombie = (Zombie) world.spawnEntity(spawnLocation, EntityType.ZOMBIE);
            zombie.setTarget(playerLocation.getWorld().getNearbyEntities(playerLocation, 10, 10, 10)
                    .stream().filter(entity -> entity instanceof org.bukkit.entity.Player)
                    .map(entity -> (org.bukkit.entity.Player) entity).findFirst().orElse(null));
        }
        myGPT.transGPT(player,ChatColor.RED + "ゾンビが大量に現れました！気を付けてください！");
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the killer is a player
        if (event.getEntity().getKiller() != null) {
            player = event.getEntity().getKiller();
        }
        if (player == null) return; // If no player killed the entity, exit early
        int commandLevel =App.getCommandLevel(player);
        if(commandLevel == 4){
            Entity entity = event.getEntity();
            // ジャイアントゾンビが死亡した場合
            if (entity instanceof Giant) {
                Giant giant = (Giant) entity;
                // ジャイアントゾンビがプレイヤーによって倒されたか確認
                if (giant.getKiller() instanceof Player) {
                    Player killer = giant.getKiller();
                    if (killer != null) {
                        // クエストクリアの通知をプレイヤーに送信
                        completeQuest(player);
                    }
                }
            }
        } else if(commandLevel == 1){
            if(player == null) return;
            if (player.getMetadata("QuestStat").get(0).asInt() == 1) {
                if (!(event.getEntity() instanceof Zombie)) return;
                Zombie zombie = (Zombie) event.getEntity();
                if (zombie.getLastDamageCause() != null &&
                        (zombie.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                                zombie.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR)) {
                    killAllZombies();
                    completeQuest(player);
                }
            }
        }

    }
    private void killAllZombies() {
        for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
            if (entity instanceof Zombie) {
                entity.remove();
            }
            if (entity instanceof Giant) {
                entity.remove();
            }
        }
    }
    public void completeQuest(Player player) {
        int quest_stat = player.getMetadata("QuestStat").getFirst().asInt();
        if(quest_stat == 1) {
            player.sendTitle("CLEAR!", "", 10, 70, 20);
            int level = App.getCommandLevel(player);
            if(level == 5) {
                return;
            }
            if (level < 4) {
                giveRewards(player);
                myGPT.transGPT(player, "コマンドレベルが上がりました！新しいクエストが受けられます！");
                if (level == 1) {
                    // プレイヤーの暗視効果を削除
                    if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        myGPT.transGPT(player, ChatColor.GREEN + "暗視効果を消去しました！");
                    } else {
                        myGPT.transGPT(player, ChatColor.YELLOW + "暗視効果は現在付与されていません。");
                    }
                    //statを初期化
                }
                updateCommandBook(player);
                //statを初期化
                player.setMetadata("QuestStat", new FixedMetadataValue(plugin, 0));
            } else if (level == 4) {
                myGPT.transGPT(player, "コマンドブックが完成しました！");
                updateCommandBook(player);
                startEndRoll(player);
                //questStatは初期化しない！！！
            }
        }
    }

    public void giveRewards(Player player) {
        int level = App.getCommandLevel(player);
        if(level == 1){
            ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
            ItemMeta meta = boots.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);  // Protection IV
                meta.addEnchant(Enchantment.MENDING, 1, true);  // Mending
                meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);  // Blast Protection IV
                meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);  // Fire Protection IV
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);  // Feather Falling IV
                meta.setDisplayName("Commander Boots");
                boots.setItemMeta(meta);
            }
            // Give the boots to the player
            player.getInventory().addItem(boots);
            // Notify the player about the reward
            myGPT.transGPT(player,"クエスト完了!コマンダーブーツを手に入れました！");
        }else if(level == 2){
            ItemStack leggins = new ItemStack(Material.NETHERITE_LEGGINGS);
            ItemMeta meta = leggins.getItemMeta();
            // Add enchantments to the boots
            if (meta != null) {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);  // Protection IV
                meta.addEnchant(Enchantment.MENDING, 1, true);  // Mending
                meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);  // Blast Protection IV
                meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);  // Fire Protection IV
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);  // Feather Falling IV
                // Set custom name for the item
                meta.setDisplayName("Commander Leggins");
                leggins.setItemMeta(meta);
            }
            // Give the boots to the player
            player.getInventory().addItem(leggins);
            // Notify the player about the reward
            myGPT.transGPT(player,"クエスト完了!コマンダーレギンスを手に入れました！");
        }else if(level == 3){
            ItemStack leggins = new ItemStack(Material.NETHERITE_CHESTPLATE);
            ItemMeta meta = leggins.getItemMeta();
            // Add enchantments to the boots
            if (meta != null) {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);  // Protection IV
                meta.addEnchant(Enchantment.MENDING, 1, true);  // Mending
                meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);  // Blast Protection IV
                meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);  // Fire Protection IV
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);  // Feather Falling IV
                // Set custom name for the item
                meta.setDisplayName("Commander ChestPlate");
                leggins.setItemMeta(meta);
            }
            // Give the boots to the player
            player.getInventory().addItem(leggins);
            // Notify the player about the reward
            myGPT.transGPT(player,"クエスト完了!コマンダーチェストプレートを手に入れました！");
        }

    }
    private void updateCommandBook(Player player) {
        ItemStack commandBook = getCommandBookFromInventory(player);
        if (commandBook != null) {
            BookMeta meta = (BookMeta) commandBook.getItemMeta();
            int pages = meta.getPageCount();
            if(App.locale.equals("en")){
                if (pages == 3) {
                    meta.addPage("Page 4: /tp\n- Usage: /tp @e[type = rabbit] @s\nTeleport rabbit to yourself.\n\n　Usage: /tp 10 20 30\nTeleport to Position x =10 y=20 z=30. ");
                } else if (pages == 4) {
                    meta.addPage("Page 5: /locate structure\n- Usage: /locate structure mansion\n- You can find Forest Mansion by locate command.");
                } else if (pages == 5) {
                    meta.addPage("Page 6: /say\n- Usage: /say hello\n- helloとさけびます！");
                    meta.addPage("Page 7: /execute\n- Usage: /execute as @a run say CommandKing!\n- All Mobs call CommandKing and CommandKing comes!");
                } else if (pages == 7) {
                    meta.addPage("Page 8: Congratulations!All commands can be used.\n");
                }
                commandBook.setItemMeta(meta);
                myGPT.transGPT(player,"コマンドブックが更新されページが追加されました！");
            }else {

                if (pages == 3) {
                    meta.addPage("4ページ目: /tp コマンド\n- 使用例: /tp @e[type = rabbit] @s\nウサギを自分の位置にテレポート。 \n\n　使用例: /tp 10 20 30\nx座標が10、y座標が20、z座標が30の地点に移動。 ");
                } else if (pages == 4) {
                    meta.addPage("5ページ目: /locate structure コマンド\n- 使用例: /locate structure mansion\n- 森の家を見つけるために使います。");
                } else if (pages == 5) {
                    meta.addPage("6ページ目: /say コマンド\n- 使用例: /say hello\n- helloとさけびます！");
                    meta.addPage("7ページ目: /execute コマンド\n- 使用例: /execute as @a run say CommandKing!\n- すべてのモブが魔王召喚と叫び、魔王を召喚します。");
                } else if (pages == 7) {
                    meta.addPage("8ページ目: おめでとう！すべてのコマンドが解放されました！\n");
                }
                commandBook.setItemMeta(meta);
                myGPT.transGPT(player,"コマンドブックが更新されページが追加されました！");
            }
        }
    }
    private ItemStack getCommandBookFromInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                BookMeta meta = (BookMeta) item.getItemMeta();
                if (meta != null && Objects.requireNonNull(meta.getTitle()).equals("CommandBook")) {
                    return item;
                }
            }
        }
        return null;
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<MetadataValue> metadata = player.getMetadata("QuestStat");
        if (metadata == null || metadata.isEmpty()) {
            // Metadata not present; handle this case (e.g., log a warning, set default value, etc.)
            return;
        }
        int status = metadata.getFirst().asInt();
        if(status == 1){//クエスト中だったら
            killAllZombies();
        }
    }
}
