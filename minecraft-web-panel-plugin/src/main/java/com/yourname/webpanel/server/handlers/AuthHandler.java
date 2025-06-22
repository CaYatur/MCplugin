package com.yourname.webpanel.server.handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AuthHandler implements HttpHandler {
    private final AuthManager authManager;
    
    public AuthHandler(JavaPlugin plugin) {
        this.authManager = new AuthManager(plugin);
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        JSONObject response = new JSONObject();
        
        try {
            if (method.equalsIgnoreCase("POST")) {
                JSONObject requestBody = getRequestBody(exchange);
                
                switch (path) {
                    case "/auth/login":
                        String username = requestBody.getString("username");
                        String password = requestBody.getString("password");
                        response = authManager.login(username, password);
                        break;
                    case "/auth/logout":
                        String sessionId = requestBody.getString("sessionId");
                        authManager.logout(sessionId);
                        response.put("status", "success");
                        response.put("message", "Çıkış yapıldı");
                        break;
                    case "/auth/check":
                        String checkSessionId = requestBody.getString("sessionId");
                        response = authManager.checkSession(checkSessionId);
                        break;
                    default:
                        response.put("status", "error");
                        response.put("message", "Unknown auth endpoint");
                        break;
                }
            } else {
                response.put("status", "error");
                response.put("message", "Only POST supported");
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        byte[] respBytes = response.toString().getBytes();
        exchange.sendResponseHeaders(200, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.close();
    }
    
    private JSONObject getRequestBody(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return new JSONObject(buffer.toString());
    }
    
    public AuthManager getAuthManager() {
        return authManager;
    }
}
