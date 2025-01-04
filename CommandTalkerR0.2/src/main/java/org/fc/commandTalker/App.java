package org.fc.commandTalker;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.fc.commandTalker.command.CommandDataLoader;
import org.fc.commandTalker.command.CommandTrackerListener;
import org.fc.commandTalker.command.CommandTrackerTask;
import org.fc.commandTalker.info.MobInfo;
import org.fc.commandTalker.quest.CommandBookQuestListener;

import java.io.*;
import java.util.*;
import java.util.List;

public class App extends JavaPlugin implements Listener {
    private final Map<Player, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_DELAY = 3000L;  // 1秒 (1000ミリ秒) のインターバル
    private final MobInfo mobInfo = new MobInfo();
    private final Random random = new Random();
    private CommandTrackerListener.CommandData commandData;
    private CommandDataLoader commandDataLoader;
    public static String locale = "";
    private MyGPT myGPT = null;
    private static final String COMMAND_TALKER_NAME = "§6Command Talker";
    private static final String COMMAND_BOOK_NAME = "CommandBook";
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // プラグインフォルダを取得
        File pluginFolder = getDataFolder();
        // フォルダが存在しない場合は作成
        if (!pluginFolder.exists()) {
            if (pluginFolder.mkdirs()) {
                getLogger().info("プラグインフォルダを作成しました: " + pluginFolder.getPath());
            } else {
                getLogger().warning("プラグインフォルダの作成に失敗しました: " + pluginFolder.getPath());
            }
        }

        //コンフィグファイルがない場合は作成
        this.saveDefaultConfig();
        this.commandData = new CommandTrackerListener.CommandData();
        this.commandDataLoader = new CommandDataLoader(this);
        // コマンドデータの読み込み
        commandData = commandDataLoader.loadCommandData();
        // コマンドの使用を監視するリスナーの登録
        CommandTrackerListener listener = new CommandTrackerListener(commandData);
        getServer().getPluginManager().registerEvents(listener, this);
        // プレイヤーが参加する際に定期的なタスクを開始
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(listener), this);
        Bukkit.getPluginManager().registerEvents(new CommandBookQuestListener(this), this);

        //lang
        locale = this.getConfig().getString("instructions");
        getLogger().info("locale=" + locale);
        myGPT = new MyGPT(this);
    }
    @Override
    public void onDisable() {
        // コマンドデータをファイルに保存
        commandDataLoader.saveCommandData(commandData);
    }

    private class PlayerJoinListener implements Listener {
        private final CommandTrackerListener listener;
        public PlayerJoinListener(CommandTrackerListener listener) {
            this.listener = listener;
        }
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            ScoreboardManager manager = player.getServer().getScoreboardManager();
            Scoreboard scoreboard = manager.getNewScoreboard();  // 新しいスコアボードを作成
            // objective が存在しない場合に新しく作成
            Objective objective = scoreboard.getObjective("commandStats");
            if (objective == null) {
                if(locale.contains("jp")){
                    objective = scoreboard.registerNewObjective("commandStats", "dummy", "コマンド使用状況");
                }else{
                    objective = scoreboard.registerNewObjective("commandStats", "dummy", "Command Stats");
                }
            }
            // プレイヤーにスコアボードを設定
            player.setScoreboard(scoreboard);
            //スコア更新を開始
            new CommandTrackerTask(listener.getCommandData(), player).runTaskTimer(App.this, 0L, 100L);
            initPlayer(event);
        }
    }
    private void initPlayer(PlayerJoinEvent event){
        Player player = event.getPlayer();
        // メタデータの設定
        player.setMetadata("QuestStat", new FixedMetadataValue(this, 0));
        player.setMetadata("QuestStat4", new FixedMetadataValue(this, 0));
        player.setMetadata("level", new FixedMetadataValue(this, 0));
        World world = event.getPlayer().getWorld();
        //Game Rule
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        //Player Setting
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        // コマンドブックがなければ追加
        PlayerInventory inventory = player.getInventory();
        ItemStack book;
        if (!hasCommandBook(player)) {
            player.setMetadata("QuestStat", new FixedMetadataValue(this,0));
            book = createCommandBook();
            inventory.addItem(book);
        }
        //コマンドレベルを計算
        int level = getCommandLevel(player);
        player.setMetadata("Level", new FixedMetadataValue(this,level));
        //コマンドトーカーをかぶっていないでかつ、インベントリにもないときだけコマンドトーカーを付与
        ItemStack commanderHelmet = createCommanTalker();
        ItemStack heldItem = player.getInventory().getHelmet();
        if (isCommandTalkerEquippedOnHead(player)) {
            myGPT.transGPT(player,"§7コマンドトーカーを装備中です。");  // 特別なメッセージ
        }else if (!hasCommandTalker(player)) {
            inventory.addItem(commanderHelmet);
            myGPT.transGPT(player, "§7コマンドトーカーを装備すると右クリックでモブと話せます。");
        }
        //QuestMessage
        String startMsg = "";
        if(level < 5) {
            if (App.locale.equals("en")) {
                startMsg = "Left Click CommandBook to Accept a Quest.";
            } else {
                startMsg = "コマンドブックを開いてクエストをうけてください。";
            }
            player.sendActionBar(startMsg);
        }
        //Title Message
        if (player.isOnline()) {
            player.sendTitle("§aMinecraft Command Talker!", "", 10, 70, 20);
        }
    }
    // コマンドブックがすでにインベントリにあるかチェック
    private boolean hasCommandBook(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                BookMeta meta = (BookMeta) item.getItemMeta();
                if (meta != null && Objects.requireNonNull(meta.getTitle()).equals(COMMAND_BOOK_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }
    // コマンドブックを作成
    private ItemStack createCommandBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(COMMAND_BOOK_NAME);
            meta.setAuthor("Notch");
            if(locale.equals("en")){
                meta.addPage("Page1: *How to use commands*\n Enter / , Write command and Press Enter-Key！\n");
                meta.addPage("Page2: /time \n- /time set day\nChange to Morning\n- /time set night\nChange to Night");
                meta.addPage("Page3: /weather\n- /weather clear\nChange to Clear\n- /weather rain\nChange to Rain\n");
                book.setItemMeta(meta);
            }else {
                meta.addPage("1ページ目: *コマンドの使い方*\n / を入力して、続けてコマンドを書き、Enterキーを押すだけ！\n");
                meta.addPage("2ページ目: /time コマンド\n- /time set day\n朝になります。\n- /time set night\n夜になります。");
                meta.addPage("3ページ目: /weather コマンド\n- /weather clear\n晴れます。\n- /weather rain\n雨になります。\n");
                book.setItemMeta(meta);
            }
        }

        return book;
    }
    private String getRandomElement(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            PlayerInventory inventory = ((Player)event.getDamager()).getInventory();
            ItemStack helmet = inventory.getHelmet();
            if (helmet != null && helmet.isSimilar(createCommanTalker())) {
                // ダメージを与えているエンティティがモブである場合
                if (event.getEntity() instanceof LivingEntity) {
                    // ダメージを無効化
                    event.setCancelled(true);
                }
            }
        }
    }
    public String convertMinecraftTimeToRealTime(long minecraftTime) {
        // Minecraft時間を秒単位に変換 1日（24000ティック）は20分 1ティック = 1/20秒 = 0.05秒
        double realWorldTimeInSeconds = minecraftTime / 20.0;
        // 分と秒を計算
        int minutes = (int) (realWorldTimeInSeconds / 60);
        int seconds = (int) (realWorldTimeInSeconds % 60);
        // 時間のフォーマットを作成
        return String.format("%02d:%02d", minutes, seconds);
    }
    private ItemStack createCommanTalker() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);  // レザーのヘルメットを使用
        ItemMeta meta = item.getItemMeta();  // アイテムのメタデータを取得
        if (meta != null) {
            meta.setDisplayName(COMMAND_TALKER_NAME);  // アイテム名
            meta.setLore(List.of("§7Wear this to hear mob voice by right-clicking"));  // ロア（説明文）
            meta.addEnchant(Enchantment.PROTECTION, 4, true);  // Protection IV
            meta.addEnchant(Enchantment.MENDING, 1, true);  // Mending
            meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);  // Blast Protection IV
            meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);  // Fire Protection IV
            item.setItemMeta(meta);  // アイテムにメタデータを設定
        }
        return item;
    }
    // プレイヤーがアイテムを捨てられないようにするイベントリスナー
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // プレイヤーが捨てようとしているアイテム
        ItemStack item = event.getItemDrop().getItemStack();
        // 本のタイトルが "CommandBook" か確認
        if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta) item.getItemMeta();
            // 本のタイトルが "CommandBook" か確認
            if (bookMeta.hasTitle() && COMMAND_BOOK_NAME.equals(bookMeta.getTitle())) {
                event.setCancelled(true);
                myGPT.transGPT(event.getPlayer(), "§4You cannot drop the CommandBook!");
            }
        }
        if (item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().equals(COMMAND_TALKER_NAME)) {
            event.setCancelled(true);  // アイテムのドロップをキャンセル
            myGPT.transGPT(event.getPlayer(), "§4You cannot drop the Command Talker!");  // メッセージを表示
        }
    }
    // モブを右クリックして声がきこえるイベント
    @EventHandler
    public void onPlayerRightClickMob(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        // インターバル内に連打した場合、処理をキャンセル
        long currentTime = System.currentTimeMillis();
        long lastTime = lastClickTime.getOrDefault(player, 0L);
        if (currentTime - lastTime < CLICK_DELAY) {
            event.setCancelled(true);
            //player.sendMessage("処理中です。");
            return;
        }
        // プレイヤーが「コマンドトーカー」を装備しているか確認
        //モブの種類を取得
        String mobtype = String.valueOf(event.getRightClicked().getType());
        // プレイヤーが装備しているヘルメットがコマンドトーカーなら下記を実行
        if (isCommandTalkerEquippedOnHead(player)) {
            myGPT.transGPT(player,mobtype + "の声がきこえます!" );
            String weather = player.getWorld().hasStorm() ? "雨" : "晴れ";
            String time = convertMinecraftTimeToRealTime(player.getWorld().getTime());
            String biomeName = player.getLocation().getBlock().getBiome().name();
            String lang = "";
            if(locale.contains("jp")){
                lang = "日本語でお願いします。";
            }else{
                lang = "in Engilish.";
            }
            getLogger().info(locale + ":" + lang);
            //コマンドレベルに合わせた応答を選択
            String iMsg = getRandomElement(Arrays.asList("「コマンドって難しいよね。ぼくたちもたまに間違えちゃうんだ。」","「コマンドブックのコマンドは絶対に間違いないからすごいよね。」"));
            String questMessage1 = getRandomElement(Arrays.asList("ゾンビは晴れると、やけしんでしまうんだよ。","「/time set day」コマンドで朝にできるから、夜もゾンビもこわくないよね。","コマンド使ったことある？/ を入力して、続けてコマンドを書き、Enterキーを押すだけだよ。","コマンドをたくさん使うと最強の剣がもらえるらしいよ。"));
            String questMessage2 = getRandomElement(Arrays.asList("「/tp @e[type=rabbit] @s」でウサギを自分のまわりに集められるよ。","ウサギはネコによわいよ。","コマンドをたくさん使うと最強の剣がもらえるらしいよ。"));
            String questMessage3 = getRandomElement(Arrays.asList("「/locate structure minecraft:mansion」で森の洋館にいけるよ。","森の洋館は暗いから、たいまつがあるといいかも。","森の洋館のチェストには宝がたくさんあるよ。","むかしはたくさんコマンド使えたのに、コマンドキングが使えなくしたらしいよ。"));
            String questMessage4 = getRandomElement(Arrays.asList("「/execute as @e run say CommandKing」でコマンドキングを呼び出せるらしい。","「/say　hello」コマンドでhelloっていえるんだよ。","むかしはたくさんコマンド使えたのに、コマンドキングが使えなくしたらしい。"));
            String questMessage5 = getRandomElement(Arrays.asList("あとは自由に遊ぶだけだね。","/kill とか使わないでね、、、","コマンドキングを倒したなんてすごいね！"));
            int cmd_num = 1;//返答に含むコマンドの数
            String mode = "time weather ";
            int level = getCommandLevel(player);
            String qMsg = "";
            if(level == 1) {
                qMsg = questMessage1;
            }else if(level == 2) {
                mode = mode + "tp ";
                qMsg= questMessage2;
            }else if(level == 3){
                mode = mode + "locate";
                qMsg = questMessage3;
            }else if(level == 4){
                mode = mode + "execute say ";
                qMsg = questMessage4;
            }else if(level == 5){
                mode = "すべてのコマンド";;
                qMsg = questMessage5;
            }
            int r = random.nextInt(4);
            if (r == 0) {
                myGPT.transGPT(player, qMsg);
                return;
            }else if (r == 1) {
                myGPT.transGPT(player, iMsg);
                return;
            }
            String tmp = "現在の天気は" + weather + "で" + "時間は" + time + "で、バイオームは" + biomeName + "です。"
                    + "あなたはマインクラフトの" + mobInfo.getAge() + "で" + mobInfo.getGender() + "で"
                    + mobInfo.getHobby() + "な" + mobtype
                    + "で、子供向けの" + lang + "で200文字以内で次の内容について教えてください。"
                    + mode + "の中から今のあなたが使いたいJava版マイクラ1.21のコマンドの使用例" + cmd_num +"個とそれに関連するマイクラの楽しみ方を教えてください。"
                    + "前置きや挨拶はいらないです。"
                    + "ただし、コマンドは「」で囲い、コマンドに{}で囲まれるjsonデータは含めないでください。"
                    + "ちなみに、summon boatを提案する時は、summon acasia_boatにですが、あまり使いすぎないでね。"
                    + lang;

            Bukkit.getScheduler().runTask(this, () -> myGPT.sendMessageToChatGPT(player, tmp));
        }
        lastClickTime.put(player, currentTime);
    }
    // プレイヤーが頭に "Command Talker" を装備しているかチェック
    public static boolean isCommandTalkerEquippedOnHead(Player player) {
        // 頭のスロットのアイテムを取得
        ItemStack helmet = player.getInventory().getHelmet();
        // 頭装備が存在しない場合、または名前が一致しない場合は false
        if (helmet == null || !helmet.hasItemMeta()) {
            return false;
        }
        // アイテムの名前が "Command Talker" か確認
        return COMMAND_TALKER_NAME.equals(helmet.getItemMeta().getDisplayName());
    }
    // プレイヤーのインベントリ内に "Command Talker" が存在するかチェック
    public static boolean hasCommandTalker(Player player) {
        // プレイヤーのインベントリを取得
        ItemStack[] inventory = player.getInventory().getContents();
        // インベントリの全アイテムをチェック
        for (ItemStack item : inventory) {
            if (item != null && item.hasItemMeta() && COMMAND_TALKER_NAME.equals(item.getItemMeta().getDisplayName())) {
                return true; // アイテムが見つかった
            }
        }
        // 見つからない場合は false
        return false;
    }
    // プレイヤーのコマンドレベルを取得（ページ数に基づく）
    public static int getCommandLevel(Player player) {
        // "commandBook" をインベントリから取得
        ItemStack book = getCommandBookFromInventory(player);
        // commandBookがインベントリにない場合は、初期レベル1を返す
        if (book == null) {
            return 1;
        }
        // BookMeta を取得してページ数を確認
        BookMeta meta = (BookMeta) book.getItemMeta();
        int level = 1; // デフォルトのレベル（ページ数が2以下の場合）
        if (meta != null) {
            int pageCount = meta.getPageCount();
            // ページ数に基づいてレベルを決定
            if (pageCount == 4) {
                level = 2;
            } else if (pageCount == 5) {
                level = 3; // 必要に応じて他のページ数に対応
            }else if (pageCount == 7) {
                level = 4;
            }else if (pageCount == 8) {
                level = 5;
            }
        }
        return level;
    }
    // インベントリから "commandBook" という名前のアイテムを取得
    public static ItemStack getCommandBookFromInventory(Player player) {
        // プレイヤーのインベントリ内のアイテムをループ
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                // アイテムが WRITTEN_BOOK 型であることを確認
                BookMeta meta = (BookMeta) item.getItemMeta();
                if (meta != null && Objects.requireNonNull(meta.getTitle()).equals(COMMAND_BOOK_NAME)) {
                    // アイテムのタイトルが "commandBook" の場合、そのアイテムを返す
                    return item;
                }
            }
        }
        // 見つからなかった場合は null を返す
        return null;
    }
}




