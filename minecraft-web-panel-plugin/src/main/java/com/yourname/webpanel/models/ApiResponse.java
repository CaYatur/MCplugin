/* filepath: c:\Users\cagan\Desktop\MCplugin\minecraft-web-panel-plugin\src\main\java\com\yourname\webpanel\models\ApiResponse.java */
package com.yourname.webpanel.models;

import org.json.JSONObject;
import org.json.JSONArray;

public class ApiResponse {
    
    public static JSONObject success(Object data) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }
    
    public static JSONObject success(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("message", message);
        return response;
    }
    
    public static JSONObject success(JSONObject data) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }
    
    public static JSONObject success(JSONArray data) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }
    
    public static JSONObject error(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
    
    public static JSONObject error(String message, int code) {
        JSONObject response = new JSONObject();
        response.put("status", "error");
        response.put("message", message);
        response.put("code", code);
        return response;
    }
    
    public static JSONObject error(String message, String details) {
        JSONObject response = new JSONObject();
        response.put("status", "error");
        response.put("message", message);
        response.put("details", details);
        return response;
    }
    
    public static JSONObject info(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "info");
        response.put("message", message);
        return response;
    }
    
    public static JSONObject info(String message, Object data) {
        JSONObject response = new JSONObject();
        response.put("status", "info");
        response.put("message", message);
        response.put("data", data);
        return response;
    }
    
    public static JSONObject warning(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "warning");
        response.put("message", message);
        return response;
    }
    
    public static JSONObject warning(String message, Object data) {
        JSONObject response = new JSONObject();
        response.put("status", "warning");
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}