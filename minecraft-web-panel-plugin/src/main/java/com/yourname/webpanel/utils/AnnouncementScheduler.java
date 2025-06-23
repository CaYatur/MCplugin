package com.yourname.webpanel.utils;

import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.models.Announcement;
import com.yourname.webpanel.server.handlers.AnnouncementHandler;
import com.yourname.webpanel.server.websocket.WebSocketHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AnnouncementScheduler {
    private final WebPanelPlugin plugin;
    private final AnnouncementHandler handler;
    private Timer timer;

    public AnnouncementScheduler(WebPanelPlugin plugin, AnnouncementHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Announcement> anns = handler.getAnnouncements();
                long now = System.currentTimeMillis();
                for (Announcement ann : anns) {
                    if (!ann.active) continue;
                    if (ann.interval > 0 && (now - ann.time) % ann.interval < 1000) {
                        broadcast(ann);
                    } else if (ann.interval == 0 && Math.abs(now - ann.time) < 1000) {
                        broadcast(ann);
                        ann.active = false;
                        handler.save();
                    }
                }
            }
        }, 0, 1000);
    }

    public void stop() {
        if (timer != null) timer.cancel();
    }

    private void broadcast(Announcement ann) {
        // Minecraft sohbetine renkli mesaj
        String color = getColor(ann.type);
        String msg = color + "[Duyuru] " + ChatColor.RESET + ann.title + ": " + ann.message;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(msg));
        // WebSocket ile web arayüzüne gönder
        org.json.JSONObject wsMsg = new org.json.JSONObject();
        wsMsg.put("type", "announcement");
        wsMsg.put("title", ann.title);
        wsMsg.put("message", ann.message);
        wsMsg.put("level", ann.type.toString().toLowerCase());
        WebSocketHandler.broadcastToAll(wsMsg);
    }

    private String getColor(Announcement.Type type) {
        switch (type) {
            case SUCCESS: return ChatColor.GREEN.toString();
            case WARNING: return ChatColor.YELLOW.toString();
            case ERROR: return ChatColor.RED.toString();
            default: return ChatColor.AQUA.toString();
        }
    }
}
