/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\java\com\yourname\webpanel\server\WebServer.java */
package com.yourname.webpanel.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.yourname.webpanel.WebPanelPlugin;
import com.yourname.webpanel.server.handlers.ApiHandler;
import com.yourname.webpanel.server.handlers.AuthHandler;
import com.yourname.webpanel.server.handlers.AnnouncementHandler;
import com.yourname.webpanel.server.handlers.ReportHandler;
import com.yourname.webpanel.server.websocket.WebSocketHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class WebServer {
    
    private final WebPanelPlugin plugin;
    private HttpServer httpServer;
    private WebSocketHandler webSocketHandler;
    private AuthHandler authHandler;
    public AnnouncementHandler announcementHandler;
    public ReportHandler reportHandler;
    private int webPort;
    private int wsPort;
    
    public WebServer(WebPanelPlugin plugin, int webPort, int wsPort) {
        this.plugin = plugin;
        this.webPort = webPort;
        this.wsPort = wsPort;
    }
    
    public void start() {
        try {
            // HTTP Server başlat
            httpServer = HttpServer.create(new InetSocketAddress(webPort), 0);
            // Static file handler
            httpServer.createContext("/", new StaticFileHandler());
            // API handler
            httpServer.createContext("/api", new ApiHandler(plugin));
            // Duyuru handler
            announcementHandler = new AnnouncementHandler(plugin);
            httpServer.createContext("/api/announcements", announcementHandler);
            // Rapor handler
            reportHandler = new ReportHandler(plugin);
            httpServer.createContext("/api/reports", reportHandler);
            // Auth handler
            authHandler = new AuthHandler(plugin);
            httpServer.createContext("/auth", authHandler);
            
            // Status endpoint
            httpServer.createContext("/status", exchange -> {
                String response = "{\"status\":\"ok\",\"service\":\"minecraft-web-panel\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });
            
            httpServer.setExecutor(Executors.newFixedThreadPool(10));
            httpServer.start();
            
            // WebSocket Server başlat
            webSocketHandler = new WebSocketHandler(plugin, wsPort);
            webSocketHandler.start();
            
            plugin.getLogger().info("Web server started on port " + webPort);
            plugin.getLogger().info("WebSocket server started on port " + wsPort);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            plugin.getLogger().info("HTTP server stopped");
        }
        
        if (webSocketHandler != null) {
            webSocketHandler.shutdown();
            plugin.getLogger().info("WebSocket server stopped");
        }
    }
    
    public int getWebPort() {
        return webPort;
    }
    
    public int getWebSocketPort() {
        return wsPort;
    }
    
    public boolean isRunning() {
        return httpServer != null && webSocketHandler != null && webSocketHandler.isRunning();
    }
    
    public AuthHandler getAuthHandler() {
        return authHandler;
    }
    
    // Static file handler sınıfı
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Root path için index.html
            if ("/".equals(path)) {
                path = "/index.html";
            }
            
            // /static/ ile başlayan istekleri web klasörüne yönlendir
            if (path.startsWith("/static/")) {
                path = path.replaceFirst("/static", "");
            }
            
            // Web klasöründeki dosyayı bul
            File webDir = new File(plugin.getDataFolder(), "web");
            File file = new File(webDir, path);
            
            // Web klasörü yoksa oluştur ve kaynaklardan kopyala
            if (!webDir.exists()) {
                webDir.mkdirs();
                // index.html ve login.html dosyalarını kopyala
                plugin.saveResource("web/index.html", false);
                plugin.saveResource("web/login.html", false);
            }
            
            // Kaynaklardan web klasörünü ve içeriğini kopyala
            copyWebResourcesIfMissing(webDir);
            
            if (file.exists() && file.isFile()) {
                // MIME type belirle
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                
                // Dosyayı oku ve gönder
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, fileBytes.length);
                exchange.getResponseBody().write(fileBytes);
            } else {
                // 404 Not Found
                String response = "404 - File not found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
            }
            
            exchange.close();
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "text/plain";
        }
        
        // Eksikse web/js, web/css ve ana dosyaları kopyala
        private void copyWebResourcesIfMissing(File webDir) {
            if (!webDir.exists()) webDir.mkdirs();
            try {
                // js klasörü ve dosyaları
                File jsDir = new File(webDir, "js");
                if (!jsDir.exists()) jsDir.mkdirs();
                for (String file : new String[]{"auth.js", "main.js", "ui.js", "websocket.js"}) {
                    File destFile = new File(jsDir, file);
                    if (!destFile.exists()) plugin.saveResource("web/js/" + file, false);
                }
                // css klasörü ve dosyası
                File cssDir = new File(webDir, "css");
                if (!cssDir.exists()) cssDir.mkdirs();
                File cssFile = new File(cssDir, "style.css");
                if (!cssFile.exists()) plugin.saveResource("web/css/style.css", false);
                // assets klasörü (varsa)
                File assetsDir = new File(webDir, "assets");
                if (!assetsDir.exists()) assetsDir.mkdirs();
                File favicon = new File(assetsDir, "favicon.ico");
                if (!favicon.exists()) plugin.saveResource("web/assets/favicon.ico", false);
                // Ana html dosyaları
                for (String file : new String[]{"index.html", "login.html"}) {
                    File destFile = new File(webDir, file);
                    if (!destFile.exists()) plugin.saveResource("web/" + file, false);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Web kaynakları kopyalanırken hata: " + e.getMessage());
            }
        }
    }
}