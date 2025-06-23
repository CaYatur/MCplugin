/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\java\com\yourname\webpanel\server\handlers\ApiHandler.java */
package com.yourname.webpanel.server.handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.yourname.webpanel.WebPanelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ApiHandler implements HttpHandler {
    
    private final WebPanelPlugin plugin;
    
    public ApiHandler(WebPanelPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        String method = exchange.getRequestMethod();
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        path = path.substring(4); // "/api" kısmını çıkar
        
        JSONObject response = new JSONObject();
        
        try {
            if ("GET".equals(method)) {
                response = handleGet(path, exchange);
            } else if ("POST".equals(method)) {
                response = handlePost(path, exchange);
            } else {
                response.put("status", "error");
                response.put("message", "Method not allowed");
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            plugin.getLogger().severe("API Error: " + e.getMessage());
        }
        
        // Response gönder
        String responseStr = response.toString();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = responseStr.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
    
    private JSONObject handleGet(String path, HttpExchange exchange) {
        JSONObject response = new JSONObject();
        
        switch (path) {
            case "/players":
                response = getPlayersInfo();
                break;
            case "/server/stats":
                response = getServerStats();
                break;
            case "/players/banned":
                response = getBannedPlayers();
                break;
            case "/players/kickall":
                response = kickAllPlayers();
                break;
            case "/players/banned-ips":
                response = getBannedIPs();
                break;
            case "/players/all-ips": // Yeni endpoint
                response = getAllPlayersWithIps();
                break;
            default:
                if (path.startsWith("/players/info/")) {
                    String playerName = path.substring("/players/info/".length());
                    response = getPlayerInfo(playerName);
                } else {
                    response.put("status", "error");
                    response.put("message", "Unknown endpoint");
                }
                break;
        }
        
        return response;
    }
    
    private JSONObject handlePost(String path, HttpExchange exchange) throws IOException {
        JSONObject response = new JSONObject();
        JSONObject requestBody = getRequestBody(exchange);
        switch (path) {
            case "/players/kick":
                response = kickPlayer(requestBody);
                break;
            case "/players/ban":
                response = banPlayer(requestBody);
                break;
            case "/players/unban":
                response = unbanPlayer(requestBody);
                break;
            case "/chat/send":
                response = sendChatMessage(requestBody);
                break;
            case "/console/execute":
                response = executeCommand(requestBody);
                break;
            default:
                response.put("status", "error");
                response.put("message", "Unknown endpoint");
                break;
        }
        return response;
    }
    
    private JSONObject getPlayersInfo() {
        JSONArray playersArray = new JSONArray();
        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUniqueId().toString());
            playerInfo.put("ping", player.getPing());
            playerInfo.put("headUrl", "https://minotar.net/avatar/" + player.getName() + "/32");
            playerInfo.put("world", player.getWorld().getName());
            playerInfo.put("x", Math.round(player.getLocation().getX() * 100.0) / 100.0);
            playerInfo.put("y", Math.round(player.getLocation().getY() * 100.0) / 100.0);
            playerInfo.put("z", Math.round(player.getLocation().getZ() * 100.0) / 100.0);
            playerInfo.put("gameMode", player.getGameMode().toString());
            playerInfo.put("health", player.getHealth());
            playerInfo.put("level", player.getLevel());
            playersArray.put(playerInfo);
        }
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", playersArray);
        return response;
    }
    
    private JSONObject getServerStats() {
        JSONObject stats = new JSONObject();
        
        // Oyuncu bilgileri
        stats.put("onlineCount", Bukkit.getOnlinePlayers().size());
        stats.put("maxPlayers", Bukkit.getMaxPlayers());
        
        // Sunucu bilgileri
        stats.put("version", Bukkit.getVersion());
        stats.put("bukkitVersion", Bukkit.getBukkitVersion());
        stats.put("name", Bukkit.getName());
        
        // Bellek kullanımı
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        JSONObject memoryInfo = new JSONObject();
        memoryInfo.put("used", memoryBean.getHeapMemoryUsage().getUsed());
        memoryInfo.put("max", memoryBean.getHeapMemoryUsage().getMax());
        memoryInfo.put("committed", memoryBean.getHeapMemoryUsage().getCommitted());
        stats.put("memoryUsage", memoryInfo);
        
        // Çalışma süresi
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        stats.put("uptime", uptime);

        // TPS ekle (Paper veya Spigot API ile, 20 ile sınırla)
        try {
            double tps = 20.0;
            if (Bukkit.getServer().getClass().getMethod("getTPS") != null) {
                double[] tpsArr = (double[]) Bukkit.getServer().getClass().getMethod("getTPS").invoke(Bukkit.getServer());
                if (tpsArr != null && tpsArr.length > 0) {
                    tps = Math.min(Math.round(tpsArr[0] * 100.0) / 100.0, 20.0);
                }
            }
            stats.put("tps", tps);
        } catch (Exception e) {
            stats.put("tps", JSONObject.NULL);
        }

        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", stats);
        return response;
    }
    
    private JSONObject getBannedPlayers() {
        JSONArray bannedArray = new JSONArray();
        try {
            // Sunucu ana dizininden banned-players.json dosyasını oku
            File file = new File("../banned-players.json");
            if (!file.exists()) file = new File("banned-players.json");
            if (file.exists()) {
                String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    JSONObject banInfo = new JSONObject();
                    banInfo.put("name", obj.optString("name"));
                    banInfo.put("uuid", obj.optString("uuid"));
                    banInfo.put("created", obj.optString("created"));
                    banInfo.put("source", obj.optString("source"));
                    banInfo.put("expires", obj.optString("expires"));
                    banInfo.put("reason", obj.optString("reason"));
                    bannedArray.put(banInfo);
                }
            }
        } catch (Exception e) {
            // Hata olursa boş dizi dön
        }
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", bannedArray);
        return response;
    }
    
    private JSONObject unbanPlayer(JSONObject requestBody) {
        String playerName = requestBody.getString("player");
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + playerName);
        });
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Player unbanned successfully");
        return response;
    }
    
    private JSONObject getPlayerInfo(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        JSONObject playerData = new JSONObject();
        String ip = "";
        if (player != null) {
            playerData.put("name", player.getName());
            playerData.put("uuid", player.getUniqueId().toString());
            playerData.put("ping", player.getPing());
            playerData.put("gameMode", player.getGameMode().toString());
            playerData.put("world", player.getWorld().getName());
            playerData.put("x", Math.round(player.getLocation().getX() * 100.0) / 100.0);
            playerData.put("y", Math.round(player.getLocation().getY() * 100.0) / 100.0);
            playerData.put("z", Math.round(player.getLocation().getZ() * 100.0) / 100.0);
            playerData.put("health", player.getHealth());
            playerData.put("hunger", player.getFoodLevel());
            playerData.put("level", player.getLevel());
            ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";
            playerData.put("ip", ip);
            playerData.put("firstPlayed", player.getFirstPlayed());
            playerData.put("lastPlayed", player.getLastPlayed());
            playerData.put("isOp", player.isOp());
            playerData.put("isWhitelisted", player.isWhitelisted());
            playerData.put("displayName", player.getDisplayName());
        } else {
            // Offline player desteği (UUID ile veya eski girişler üzerinden)
            playerData.put("name", playerName);
            playerData.put("uuid", "");
            playerData.put("ping", -1);
            playerData.put("gameMode", "");
            playerData.put("world", "");
            playerData.put("x", 0);
            playerData.put("y", 0);
            playerData.put("z", 0);
            playerData.put("health", 0);
            playerData.put("hunger", 0);
            playerData.put("level", 0);
            java.util.Set<String> ips = com.yourname.webpanel.utils.PlayerUtils.getPlayerIps(playerName);
            ip = (ips != null && !ips.isEmpty()) ? ips.iterator().next() : "";
            playerData.put("ip", ip);
            playerData.put("firstPlayed", "");
            playerData.put("lastPlayed", "");
            playerData.put("isOp", org.bukkit.Bukkit.getOfflinePlayer(playerName).isOp());
            playerData.put("isWhitelisted", false);
            playerData.put("displayName", playerName);
        }
        // Alt hesaplar (aynı IP) - hem online hem offline için
        org.json.JSONArray altHesaplar = new org.json.JSONArray();
        if (ip != null && !ip.isEmpty()) {
            java.util.Set<String> allPlayers = com.yourname.webpanel.utils.PlayerUtils.getPlayersByIp(ip);
            for (String altName : allPlayers) {
                if (!altName.equalsIgnoreCase(playerName)) {
                    altHesaplar.put(altName);
                }
            }
        }
        playerData.put("altHesaplar", altHesaplar);
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", playerData);
        return response;
    }
    
    private JSONObject kickPlayer(JSONObject requestBody) {
        String playerName = requestBody.getString("player");
        String reason = requestBody.optString("reason", "Kicked by web panel");
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Player not found");
            return response;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer(reason);
        });

        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Player kicked successfully");
        return response;
    }
    
    private JSONObject banPlayer(JSONObject requestBody) {
        String playerName = requestBody.getString("player");
        String reason = requestBody.optString("reason", "Banned by web panel");
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Konsol üzerinden /ban komutunu parametreyle çalıştır
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + playerName + " " + reason);
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.kickPlayer("You have been banned: " + reason);
            }
        });
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Player banned successfully");
        return response;
    }
    
    private JSONObject sendChatMessage(JSONObject requestBody) {
        String message = requestBody.getString("message");
        String sender = requestBody.optString("sender", "Web Panel");
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage("§6[Panel] §f" + sender + ": §r" + message);
        });
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Message sent successfully");
        return response;
    }
    
    private JSONObject executeCommand(JSONObject requestBody) {
        String command = requestBody.getString("command");
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute command: " + command);
            }
        });

        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Command executed successfully");
        return response;
    }
    
    private JSONObject getRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        return new JSONObject(buffer.toString());
    }

    private JSONObject kickAllPlayers() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer("Tüm oyuncular web panelden atıldı.");
            }
        });
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", "Tüm oyuncular atıldı");
        return response;
    }

    private JSONObject getBannedIPs() {
        // Paper/Bukkit API'de doğrudan IP ban listesi yok, örnek olarak boş dizi dönülüyor
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", new org.json.JSONArray());
        return response;
    }

    private JSONObject getAllPlayersWithIps() {
        JSONArray playersArray = new JSONArray();
        Set<String> allPlayers = com.yourname.webpanel.utils.PlayerUtils.getAllPlayers();
        for (String playerName : allPlayers) {
            JSONObject obj = new JSONObject();
            obj.put("name", playerName);
            obj.put("ips", new JSONArray(com.yourname.webpanel.utils.PlayerUtils.getPlayerIps(playerName)));
            playersArray.put(obj);
        }
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", playersArray);
        return response;
    }
}