package org.fc.commandTalker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MyGPT {
    private final JavaPlugin plugin;
    public MyGPT(JavaPlugin plugin){
        this.plugin = plugin;
    }
    public void transGPT(Player player, String userMessage){
        String lang;
        if(App.locale.equals("en")){
            lang = "英語";
            sendMessageToChatGPT(player, userMessage + "を" + lang + "に翻訳して結果だけを返してください。");
        }else{
            player.sendMessage(userMessage);
        }
    }
    public void sendMessageToChatGPT(Player player, String userMessage) {
        String API_KEY = this.plugin.getConfig().getString("apikey");
        String API_URL = "https://api.openai.com/v1/chat/completions";

        try {
            // HTTPクライアントの作成
            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {

                // メッセージの構築
                String messagesJson = String.format(
                        "[{\"role\":\"system\",\"content\":\"あなたはジャーナリストです。\"}," +
                                "{\"role\":\"user\",\"content\":\"%s\"}]",
                        userMessage.replace("\"", "\\\"")
                );

                // リクエストボディの作成
                String requestBody = String.format(
                        "{\"model\":\"gpt-4o\",\"messages\":%s,\"temperature\":  1.0,\"max_tokens\":  150,\"frequency_penalty\":  2.0,\"presence_penalty\":  2.0}",
                        messagesJson
                );

                // リクエストの作成
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // リクエストの送信とレスポンスの取得
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            // ステータスコードの確認
            if (response.statusCode() != 200) {
                player.sendMessage("ChatGPTとの通信中にエラーが発生しました。");
                return;
            }

            // レスポンスの処理
            String responseBody = response.body();
            String chatGPTResponse = parseResponse(responseBody);

            // プレイヤーへのメッセージ送信
            player.sendMessage(chatGPTResponse);

        } catch (IOException | InterruptedException e) {
            player.sendMessage("ChatGPTとの通信中にエラーが発生しました。");
            e.printStackTrace();
        }
    }
    private String parseResponse(String jsonResponse) {
        return extractContent(jsonResponse);
    }
    private String extractContent(String jsonResponse) {
        // Gsonを使用してレスポンスをパース
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        // "choices"配列の中の最初のアイテムから"message" -> "content"を取得
        if (jsonObject.has("choices")) {
            JsonElement choices = jsonObject.get("choices");
            if (choices.isJsonArray() && choices.getAsJsonArray().size() > 0) {
                JsonObject firstChoice = choices.getAsJsonArray().get(0).getAsJsonObject();
                if (firstChoice.has("message") && firstChoice.getAsJsonObject("message").has("content")) {
                    return firstChoice.getAsJsonObject("message").get("content").getAsString();
                }
            }
        }
        return "レスポンスにcontentが含まれていません。";
    }
}
