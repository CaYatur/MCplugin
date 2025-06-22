package com.yourname.webpanel.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDefaultFiles() {
        // Config.yml oluştur
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Default config.yml created!");
        }

        // Plugin.yml otomatik olarak JAR'dan okunur
        plugin.reloadConfig();

        // Config değerlerini kontrol et ve varsayılanları ayarla
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("server.port")) {
            config.set("server.port", 8080);
        }

        if (!config.contains("server.websocket-port")) {
            config.set("server.websocket-port", 8081);
        }

        if (!config.contains("web.auto-create-files")) {
            config.set("web.auto-create-files", true);
        }

        if (!config.contains("web.allow-console-commands")) {
            config.set("web.allow-console-commands", true);
        }

        if (!config.contains("security.api-key")) {
            config.set("security.api-key", "your-api-key-here");
        }

        plugin.saveConfig();
    }
}