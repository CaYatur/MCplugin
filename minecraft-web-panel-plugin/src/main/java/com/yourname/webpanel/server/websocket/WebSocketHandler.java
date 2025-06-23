/* filepath: src/main/java/com/yourname/webpanel/server/websocket/WebSocketHandler.java */
package com.yourname.webpanel.server.websocket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

public class WebSocketHandler extends WebSocketServer {
    
    private static final CopyOnWriteArraySet<WebSocket> connections = new CopyOnWriteArraySet<>();
    private final JavaPlugin plugin;
    
    public WebSocketHandler(JavaPlugin plugin, int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.plugin = plugin;
    }
    
    @Override
    public void onStart() {
        plugin.getLogger().info("WebSocket server started on port " + getPort());
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        plugin.getLogger().info("WebSocket client connected from " + conn.getRemoteSocketAddress());
        // Log all handshake headers for debugging
        StringBuilder headers = new StringBuilder("Handshake headers: ");
        Iterator<String> it = handshake.iterateHttpFields();
        while (it.hasNext()) {
            String key = it.next();
            headers.append(key).append(": ").append(handshake.getFieldValue(key)).append(", ");
        }
        plugin.getLogger().info(headers.toString());
        String origin = handshake.getFieldValue("Origin");
        if (origin == null) origin = "(none)";
        plugin.getLogger().info("Handshake Origin: " + origin);
        // İlk bağlantıda sunucu bilgilerini gönder
        sendServerInfo(conn);
        sendPlayerList(conn);
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        plugin.getLogger().info("WebSocket client disconnected: " + reason);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject data = new JSONObject(message);
            handleMessage(conn, data);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid WebSocket message: " + message);
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        plugin.getLogger().log(Level.WARNING, "WebSocket error (" + (conn != null ? conn.getRemoteSocketAddress() : "server") + ")", ex);
    }
    
    private void handleMessage(WebSocket conn, JSONObject data) {
        String type = data.optString("type");
        
        switch (type) {
            case "ping":
                JSONObject pong = new JSONObject();
                pong.put("type", "pong");
                pong.put("timestamp", System.currentTimeMillis());
                conn.send(pong.toString());
                break;
                
            case "getPlayers":
                sendPlayerList(conn);
                break;
                
            case "getServerStats":
                sendServerInfo(conn);
                break;
                
            case "chat_message":
                // Web arayüzünden gelen chat mesajını oyun sohbetine yazdır
                String playerName = data.optString("player");
                String chatMessage = data.optString("message");
                if (playerName != null && !playerName.isEmpty() && chatMessage != null && !chatMessage.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.broadcastMessage("§6[Panel] §f" + playerName + ": §r" + chatMessage);
                    });
                }
                break;
                
            case "cheat_settings":
                // Panelden gelen anti-cheat ayarlarını işle
                org.json.JSONObject settings = data.optJSONObject("settings");
                if (settings != null) {
                    com.yourname.webpanel.listeners.AntiCheatListener.handleCheatSettings(settings);
                }
                break;
                
            default:
                plugin.getLogger().info("Unknown WebSocket message type: " + type);
        }
    }
    
    private void sendServerInfo(WebSocket conn) {
        JSONObject message = new JSONObject();
        message.put("type", "serverInfo");
        
        JSONObject data = new JSONObject();
        data.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        data.put("maxPlayers", Bukkit.getMaxPlayers());
        data.put("version", Bukkit.getVersion());
        data.put("bukkitVersion", Bukkit.getBukkitVersion());
        
        // Bellek kullanımı
        Runtime runtime = Runtime.getRuntime();
        JSONObject memory = new JSONObject();
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());
        memory.put("free", runtime.freeMemory());
        data.put("memory", memory);
        
        message.put("data", data);
        conn.send(message.toString());
    }
    
    private void sendPlayerList(WebSocket conn) {
        JSONObject message = new JSONObject();
        message.put("type", "playerList");
        
        JSONObject data = new JSONObject();
        data.put("players", getPlayersArray());
        
        message.put("data", data);
        conn.send(message.toString());
    }
    
    private org.json.JSONArray getPlayersArray() {
        org.json.JSONArray players = new org.json.JSONArray();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            JSONObject playerData = new JSONObject();
            playerData.put("name", player.getName());
            playerData.put("uuid", player.getUniqueId().toString());
            playerData.put("ping", player.getPing());
            playerData.put("world", player.getWorld().getName());
            playerData.put("gameMode", player.getGameMode().toString());
            playerData.put("health", player.getHealth());
            playerData.put("headUrl", "https://minotar.net/avatar/" + player.getName() + "/32");
            players.put(playerData);
        }
        
        return players;
    }
    
    // Static broadcast methods
    public static void broadcastToAll(JSONObject message) {
        String messageStr = message.toString();
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                try {
                    conn.send(messageStr);
                } catch (Exception e) {
                    connections.remove(conn);
                }
            } else {
                connections.remove(conn);
            }
        }
    }
    
    public static void sendPlayerJoin(String playerName) {
        JSONObject message = new JSONObject();
        message.put("type", "playerJoin");
        message.put("player", playerName);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public static void sendPlayerLeave(String playerName) {
        JSONObject message = new JSONObject();
        message.put("type", "playerLeave");
        message.put("player", playerName);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public static void sendChatMessage(String playerName, String chatMessage) {
        JSONObject message = new JSONObject();
        message.put("type", "chat_message"); // frontend ile tam uyumlu
        message.put("player", playerName);
        message.put("message", chatMessage);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public static void sendPlayerKicked(String playerName, String reason) {
        JSONObject message = new JSONObject();
        message.put("type", "playerKicked");
        message.put("player", playerName);
        message.put("reason", reason);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public static void sendCommandExecuted(String command) {
        JSONObject message = new JSONObject();
        message.put("type", "commandExecuted");
        message.put("command", command);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public static void sendConsoleOutput(String output, String level) {
        JSONObject message = new JSONObject();
        message.put("type", "consoleOutput");
        message.put("message", output);
        message.put("level", level);
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }
    
    public void shutdown() {
        try {
            stop(1000);
        } catch (InterruptedException e) {
            plugin.getLogger().warning("WebSocket server shutdown interrupted");
        }
    }
    
    public boolean isRunning() {
        return true; // WebSocket sunucusu çalışıyor kabul edilir
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
}