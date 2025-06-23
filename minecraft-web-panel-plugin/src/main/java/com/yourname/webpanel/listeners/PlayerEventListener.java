package com.yourname.webpanel.listeners;

import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.server.websocket.WebSocketHandler;
import com.yourname.webpanel.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;

public class PlayerEventListener implements Listener {
    private final WebPanelPlugin plugin;
    public PlayerEventListener(WebPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String ip = event.getPlayer().getAddress() != null ? event.getPlayer().getAddress().getAddress().getHostAddress() : null;
        if (ip != null) {
            PlayerUtils.recordPlayerIp(plugin, playerName, ip);
        }
        // WebSocket'e oyuncu katılma mesajı gönder
        WebSocketHandler.sendPlayerJoin(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        
        // WebSocket'e oyuncu ayrılma mesajı gönder
        WebSocketHandler.sendPlayerLeave(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        
        String playerName = event.getPlayer().getName();
        String reason = event.getReason();
        
        // WebSocket'e oyuncu atılma mesajı gönder
        WebSocketHandler.sendPlayerKicked(playerName, reason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        
        String playerName = event.getPlayer().getName();
        String message = event.getMessage();
        
        // WebSocket'e chat mesajı gönder
        WebSocketHandler.sendChatMessage(playerName, message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        
        // WebSocket'e konsol komutu gönder - 1 parametre ile
        WebSocketHandler.sendCommandExecuted(command);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoteServerCommand(RemoteServerCommandEvent event) {
        String command = event.getCommand();
        // WebSocket'e remote komut gönder - 1 parametre ile
        WebSocketHandler.sendCommandExecuted(command);
    }
}
