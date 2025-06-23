package com.yourname.webpanel.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.models.Announcement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnnouncementHandler implements HttpHandler {
    private final WebPanelPlugin plugin;
    private final List<Announcement> announcements = new CopyOnWriteArrayList<>();
    private final File dataFile;

    public AnnouncementHandler(WebPanelPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "announcements.json");
        load();
    }

    public List<Announcement> getAnnouncements() { return announcements; }

    public void addAnnouncement(Announcement ann) {
        announcements.add(ann);
        save();
    }

    public void removeAnnouncement(int idx) {
        if (idx >= 0 && idx < announcements.size()) {
            announcements.remove(idx);
            save();
        }
    }

    public void save() {
        JSONArray arr = new JSONArray();
        for (Announcement a : announcements) arr.put(a.toJson());
        try (FileWriter fw = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
            fw.write(arr.toString(2));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        announcements.clear();
        if (!dataFile.exists()) return;
        try (FileReader fr = new FileReader(dataFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            int c; while ((c = fr.read()) != -1) sb.append((char)c);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                announcements.add(Announcement.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, 0); exchange.close(); return;
        }
        try {
            if ("GET".equals(method)) {
                JSONArray arr = new JSONArray();
                for (Announcement a : announcements) arr.put(a.toJson());
                response.put("status", "success");
                response.put("data", arr);
            } else if ("POST".equals(method)) {
                JSONObject req = new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String action = req.optString("action", "add");
                if ("add".equals(action)) {
                    Announcement ann = Announcement.fromJson(req.getJSONObject("announcement"));
                    addAnnouncement(ann);
                    response.put("status", "success");
                } else if ("remove".equals(action)) {
                    int idx = req.optInt("index", -1);
                    removeAnnouncement(idx);
                    response.put("status", "success");
                } else {
                    response.put("status", "error");
                    response.put("message", "Unknown action");
                }
            } else {
                response.put("status", "error");
                response.put("message", "Method not allowed");
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
