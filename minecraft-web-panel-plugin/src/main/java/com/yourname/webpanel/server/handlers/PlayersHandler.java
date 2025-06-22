package com.yourname.webpanel.server.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

public class PlayersHandler {

    public JSONObject getPlayersInfo() {
        List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        JSONArray playersArray = new JSONArray();

        for (Player player : players) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", player.getName());
            playerInfo.put("headUrl", getPlayerHeadUrl(player));
            playersArray.put(playerInfo);
        }

        JSONObject response = new JSONObject();
        response.put("players", playersArray);
        return response;
    }

    public void updatePlayerList() {
        // WebSocket üzerinden oyuncu listesini güncelleme metodu
        JSONObject playersInfo = getPlayersInfo();
        // WebSocket handler'a gönderilecek
        System.out.println("Player list updated: " + playersInfo.toString());
    }

    private String getPlayerHeadUrl(Player player) {
        return "https://minotar.net/avatar/" + player.getName() + "/32";
    }
}