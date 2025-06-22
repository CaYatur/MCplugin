package com.yourname.webpanel.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.server.WebServer;

public class WebPanelCommand implements CommandExecutor {

    private final WebPanelPlugin plugin; // WebPanelPlugin tipini kullan
    private final WebServer webServer;

    public WebPanelCommand(WebPanelPlugin plugin, WebServer webServer) {
        this.plugin = plugin;
        this.webServer = webServer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6Web Panel Komutları:");
            sender.sendMessage("§e/webpanel status §7- Sunucu durumunu göster");
            sender.sendMessage("§e/webpanel reload §7- Plugin'i yeniden yükle");
            sender.sendMessage("§e/webpanel setpass <şifre> §7- Web panel şifresi belirle");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                sender.sendMessage("§6=== Web Panel Durumu ===");
                sender.sendMessage("§eWeb Sunucu: §a" + (webServer != null ? "Aktif" : "Kapalı"));
                if (webServer != null) {
                    sender.sendMessage("§eWeb Port: §a" + webServer.getWebPort());
                    sender.sendMessage("§eWebSocket Port: §a" + webServer.getWebSocketPort());
                }
                break;
                
            case "reload":
                if (!sender.hasPermission("webpanel.admin")) {
                    sender.sendMessage("§cBu komutu kullanmak için yetkiniz yok!");
                    return true;
                }
                sender.sendMessage("§eWeb panel yeniden yükleniyor...");
                plugin.reloadConfig();
                sender.sendMessage("§aWeb panel yeniden yüklendi!");
                break;
                
            case "setpass":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cBu komut sadece oyuncular tarafından kullanılabilir!");
                    return true;
                }
                
                Player player = (Player) sender;
                if (!player.isOp()) {
                    sender.sendMessage("§cBu komutu kullanmak için operatör olmalısınız!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cKullanım: /webpanel setpass <şifre>");
                    return true;
                }
                
                String password = args[1];
                if (password.length() < 4) {
                    sender.sendMessage("§cŞifre en az 4 karakter olmalıdır!");
                    return true;
                }
                
                // AuthManager'dan şifre belirleme
                if (plugin.getWebServer() != null && plugin.getWebServer().getAuthHandler() != null) {
                    plugin.getWebServer().getAuthHandler().getAuthManager().setOperatorPassword(player.getName(), password);
                    sender.sendMessage("§aWeb panel şifreniz başarıyla belirlendi!");
                    sender.sendMessage("§eKullanıcı adınız: §a" + player.getName());
                } else {
                    sender.sendMessage("§cWeb panel henüz başlatılmadı!");
                }
                break;
                
            default:
                sender.sendMessage("§cGeçersiz komut! /webpanel help");
                break;
        }
        return true;
    }
}