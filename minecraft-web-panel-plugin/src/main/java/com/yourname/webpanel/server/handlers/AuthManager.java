package com.yourname.webpanel.server.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private final JavaPlugin plugin;
    private final Map<String, String> operatorPasswords = new ConcurrentHashMap<>(); // player -> password
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>(); // sessionId -> playerName

    public AuthManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadOperatorPasswords();
    }

    private void loadOperatorPasswords() {
        // Config'den operatör şifreleri yükle
        if (plugin.getConfig().contains("web-panel.operators")) {
            Map<String, Object> operators = plugin.getConfig().getConfigurationSection("web-panel.operators").getValues(false);
            for (Map.Entry<String, Object> entry : operators.entrySet()) {
                operatorPasswords.put(entry.getKey(), entry.getValue().toString());
            }
        }
        plugin.getLogger().info("Loaded " + operatorPasswords.size() + " operator passwords");
    }

    public void setOperatorPassword(String playerName, String password) {
        operatorPasswords.put(playerName, password);
        plugin.getConfig().set("web-panel.operators." + playerName, password);
        plugin.saveConfig();
        plugin.getLogger().info("Password set for operator: " + playerName);
    }

    public JSONObject login(String username, String password) {
        JSONObject response = new JSONObject();

        // Oyuncunun operatör olduğunu kontrol et (çevrimiçi olma şartı kaldırıldı)
        boolean isOp = false;
        Player player = Bukkit.getPlayer(username);
        if (player != null) {
            isOp = player.isOp();
        } else {
            // Oyuncu çevrimdışıysa, config'den veya Bukkit API'den op kontrolü
            isOp = Bukkit.getOfflinePlayer(username).isOp();
        }
        if (!isOp) {
            response.put("status", "error");
            response.put("message", "Bu kullanıcı operatör değil");
            return response;
        }

        // Şifre kontrolü
        String storedPassword = operatorPasswords.get(username);
        if (storedPassword == null) {
            response.put("status", "error");
            response.put("message", "Bu operatör için şifre belirlenmemiş. /webpanel setpass <şifre> komutu ile şifre belirleyin");
            return response;
        }

        if (!storedPassword.equals(password)) {
            response.put("status", "error");
            response.put("message", "Hatalı şifre");
            return response;
        }

        // Session oluştur
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, username);

        response.put("status", "success");
        response.put("sessionId", sessionId);
        response.put("username", username);
        response.put("message", "Giriş başarılı");

        plugin.getLogger().info("Web panel login: " + username);
        return response;
    }

    public boolean isValidSession(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    public String getUsernameFromSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public void logout(String sessionId) {
        String username = activeSessions.remove(sessionId);
        if (username != null) {
            plugin.getLogger().info("Web panel logout: " + username);
        }
    }

    public JSONObject checkSession(String sessionId) {
        JSONObject response = new JSONObject();
        String username = getUsernameFromSession(sessionId);

        if (username != null) {
            response.put("status", "success");
            response.put("username", username);
            response.put("valid", true);
        } else {
            response.put("status", "error");
            response.put("message", "Geçersiz oturum");
            response.put("valid", false);
        }

        return response;
    }
}
