package com.yourname.webpanel.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.yourname.webpanel.server.handlers.PlayersHandler;

public class PlayerJoinLeaveListener implements Listener {

    private final PlayersHandler playersHandler;

    public PlayerJoinLeaveListener(PlayersHandler playersHandler) {
        this.playersHandler = playersHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playersHandler.updatePlayerList();
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        playersHandler.updatePlayerList();
    }
}