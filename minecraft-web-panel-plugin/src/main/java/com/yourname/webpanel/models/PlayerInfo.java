package com.yourname.webpanel.models;

public class PlayerInfo {
    private String name;
    private String headUrl;

    public PlayerInfo(String name, String headUrl) {
        this.name = name;
        this.headUrl = headUrl;
    }

    public String getName() {
        return name;
    }

    public String getHeadUrl() {
        return headUrl;
    }
}