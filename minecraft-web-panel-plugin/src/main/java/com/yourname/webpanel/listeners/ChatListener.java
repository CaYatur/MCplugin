package com.yourname.webpanel.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.yourname.webpanel.server.handlers.ChatHandler;

public class ChatListener implements Listener {

    private final ChatHandler chatHandler;

    public ChatListener(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = event.getMessage();
        chatHandler.forwardChatMessage(playerName, message);
    }
}