package com.yourname.webpanel.server.handlers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatHandler {

    public void sendMessageToChat(String message) {
        Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendMessageToPlayer(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendMessageToConsole(String message) {
        CommandSender console = Bukkit.getServer().getConsoleSender();
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void forwardChatMessage(String playerName, String message) {
        // WebSocket üzerinden chat mesajını web paneline gönderme
        String formattedMessage = message;
        com.yourname.webpanel.server.websocket.WebSocketHandler.sendChatMessage(playerName, formattedMessage);
    }
}