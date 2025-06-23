package com.yourname.webpanel.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.models.PlayerReport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReportHandler implements HttpHandler {
    private final WebPanelPlugin plugin;
    private final List<PlayerReport> reports = new CopyOnWriteArrayList<>();
    private final File dataFile;

    public ReportHandler(WebPanelPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "reports.json");
        load();
    }

    public List<PlayerReport> getReports() { return reports; }

    public void addReport(PlayerReport r) {
        reports.add(r);
        save();
    }

    public void resolveReport(int idx) {
        if (idx >= 0 && idx < reports.size()) {
            reports.remove(idx);
            save();
        }
    }

    public void save() {
        JSONArray arr = new JSONArray();
        for (PlayerReport r : reports) arr.put(r.toJson());
        try (FileWriter fw = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
            fw.write(arr.toString(2));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        reports.clear();
        if (!dataFile.exists()) return;
        try (FileReader fr = new FileReader(dataFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            int c; while ((c = fr.read()) != -1) sb.append((char)c);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                reports.add(PlayerReport.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String addReportWithChecks(PlayerReport r) {
        // Sebep zorunlu
        if (r.reason == null || r.reason.trim().isEmpty()) {
            return "Sebep zorunlu";
        }
        // Kendini reportlayamaz
        if (r.reporter != null && r.reported != null && r.reporter.equalsIgnoreCase(r.reported)) {
            return "Kendinizi reportlayamazsınız";
        }
        // Operatörler reportlanamaz
        if (plugin.getServer().getPlayer(r.reported) != null && plugin.getServer().getPlayer(r.reported).isOp()) {
            return "Operatörler reportlanamaz";
        }
        // Aynı kişi aynı kişiyi tekrar tekrar reportlayamaz (çözülene kadar)
        for (PlayerReport rep : reports) {
            if (!rep.resolved && rep.reporter.equalsIgnoreCase(r.reporter) && rep.reported.equalsIgnoreCase(r.reported)) {
                return "Bu kişiyi zaten report ettiniz. Çözülene kadar tekrar reportlayamazsınız.";
            }
        }
        reports.add(r);
        save();
        return null;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, 0); exchange.close(); return;
        }
        try {
            if ("GET".equals(method)) {
                JSONArray arr = new JSONArray();
                for (PlayerReport r : reports) arr.put(r.toJson());
                response.put("status", "success");
                response.put("data", arr);
            } else if ("POST".equals(method)) {
                JSONObject req = new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String action = req.optString("action", "add");
                if ("add".equals(action)) {
                    PlayerReport r = PlayerReport.fromJson(req.getJSONObject("report"));
                    String err = addReportWithChecks(r);
                    if (err != null) {
                        response.put("status", "error");
                        response.put("message", err);
                    } else {
                        response.put("status", "success");
                    }
                } else if ("resolve".equals(action)) {
                    int idx = req.optInt("index", -1);
                    resolveReport(idx);
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
