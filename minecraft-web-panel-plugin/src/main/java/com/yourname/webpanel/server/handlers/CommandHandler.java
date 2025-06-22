package com.yourname.webpanel.server.handlers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandHandler {

    public void executeCommand(String command, CommandSender sender) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("minecraft-web-panel-plugin"), () -> {
            Bukkit.dispatchCommand(sender, command);
        });
    }
}