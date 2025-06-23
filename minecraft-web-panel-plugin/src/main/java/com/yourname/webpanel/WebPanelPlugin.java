package com.yourname.webpanel;

import com.yourname.webpanel.commands.WebPanelCommand;
import com.yourname.webpanel.listeners.AntiCheatListener;
import com.yourname.webpanel.listeners.PlayerEventListener;
import com.yourname.webpanel.server.WebServer;
import com.yourname.webpanel.server.handlers.AnnouncementHandler;
import com.yourname.webpanel.server.websocket.ConsoleLogHandler;
import com.yourname.webpanel.utils.AnnouncementScheduler;
import com.yourname.webpanel.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class WebPanelPlugin extends JavaPlugin {

    private WebServer webServer;
    private ConfigManager configManager;
    private AnnouncementScheduler announcementScheduler;

    @Override
    public void onEnable() {
        // Plugin başlatma logu
        getLogger().info("Minecraft Web Panel Plugin starting...");

        // Config yöneticisini başlat
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Web sunucusunu başlat
        int webPort = getConfig().getInt("server.web-port", 8080);
        int wsPort = getConfig().getInt("server.websocket-port", 8081);

        webServer = new WebServer(this, webPort, wsPort);
        webServer.start();
        // Duyuru zamanlayıcıyı başlat
        AnnouncementHandler announcementHandler = webServer.announcementHandler;
        announcementScheduler = new AnnouncementScheduler(this, announcementHandler);
        announcementScheduler.start();

        // Event listener'ları kaydet
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new AntiCheatListener(this), this);

        // Komutları kaydet
        getCommand("webpanel").setExecutor(new WebPanelCommand(this, webServer));

        // Konsol çıktısını WebSocket ile web arayüzüne aktar
        Logger rootLogger = Logger.getLogger("");
        ConsoleLogHandler wsLogHandler = new ConsoleLogHandler();
        wsLogHandler.setFormatter(rootLogger.getHandlers()[0].getFormatter());
        rootLogger.addHandler(wsLogHandler);

        getLogger().info("Minecraft Web Panel Plugin enabled successfully!");
        getLogger().info("Web interface: http://localhost:" + webPort);
        getLogger().info("WebSocket: ws://localhost:" + wsPort);
    }

    @Override
    public void onDisable() {
        // Web sunucusunu durdur
        if (webServer != null) {
            webServer.stop();
        }

        getLogger().info("Minecraft Web Panel Plugin disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}