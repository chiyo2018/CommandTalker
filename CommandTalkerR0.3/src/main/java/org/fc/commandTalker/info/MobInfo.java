package org.fc.commandTalker.info;

public class MobInfo {
    private String personality = "勇敢";
    private String gender = "男性";
    private String origin = "都会";
    private String age = "30";
    private String hobby = "コマンド好き";

    // コンストラクタ: モンスターの情報を設定
    public MobInfo(String personality, String gender, String origin, String age, String hobby) {
        this.personality = personality;
        this.gender = gender;
        this.origin = origin;
        this.age = age;
        this.hobby = hobby;
    }
    // デフォルトのコンストラクタ
    public MobInfo() {
    }
    // モンスターの情報を取得するためのゲッターとセッター
    public String getPersonality() {
        return personality;
    }
    public void setPersonality(String personality) {
        this.personality = personality;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getOrigin() {
        return origin;
    }
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }
    public String getHobby() {
        return hobby;
    }
    public void setHobby(String hobby) {
        this.hobby = hobby;
    }
}

