package com.yourname.webpanel.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class PlayerUtils {

    private static final String IP_DATA_FILE = "player_ips.json";
    private static Map<String, Set<String>> playerIpMap = new HashMap<>();
    private static boolean loaded = false;

    public static void loadPlayerIpData(JavaPlugin plugin) {
        if (loaded) return;
        File file = new File(plugin.getDataFolder(), IP_DATA_FILE);
        if (!file.exists()) {
            loaded = true;
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            JSONObject obj = new JSONObject(sb.toString());
            for (String player : obj.keySet()) {
                JSONArray ips = obj.getJSONArray(player);
                Set<String> ipSet = new HashSet<>();
                for (int i = 0; i < ips.length(); i++) {
                    ipSet.add(ips.getString(i));
                }
                playerIpMap.put(player, ipSet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        loaded = true;
    }

    public static void savePlayerIpData(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), IP_DATA_FILE);
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, Set<String>> entry : playerIpMap.entrySet()) {
            obj.put(entry.getKey(), new JSONArray(entry.getValue()));
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(obj.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recordPlayerIp(JavaPlugin plugin, String playerName, String ip) {
        loadPlayerIpData(plugin);
        Set<String> ips = playerIpMap.getOrDefault(playerName, new HashSet<>());
        if (!ips.contains(ip)) {
            ips.add(ip);
            playerIpMap.put(playerName, ips);
            savePlayerIpData(plugin);
        }
    }

    public static Set<String> getPlayersByIp(String ip) {
        Set<String> players = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : playerIpMap.entrySet()) {
            if (entry.getValue().contains(ip)) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    public static List<String> getOnlinePlayers() {
        List<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    public static String getPlayerHeadUrl(Player player) {
        // This method should return the URL of the player's head.
        // For now, we will return a placeholder URL.
        return "https://minotar.net/avatar/" + player.getName();
    }

    public static Set<String> getAllPlayers() {
        loadPlayerIpData(null);
        return Collections.unmodifiableSet(playerIpMap.keySet());
    }

    public static Set<String> getPlayerIps(String playerName) {
        loadPlayerIpData(null);
        return playerIpMap.getOrDefault(playerName, new HashSet<>());
    }
}