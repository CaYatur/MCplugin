package com.yourname.webpanel.models;

import org.json.JSONObject;

public class Announcement {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }
    public String title;
    public String message;
    public Type type;
    public long time; // Unix timestamp (ms)
    public long interval; // ms, 0 ise tek seferlik
    public boolean active;

    public Announcement(String title, String message, Type type, long time, long interval, boolean active) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.time = time;
        this.interval = interval;
        this.active = active;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("title", title);
        obj.put("message", message);
        obj.put("type", type.toString());
        obj.put("time", time);
        obj.put("interval", interval);
        obj.put("active", active);
        return obj;
    }

    public static Announcement fromJson(JSONObject obj) {
        return new Announcement(
            obj.optString("title"),
            obj.optString("message"),
            Type.valueOf(obj.optString("type", "INFO")),
            obj.optLong("time", System.currentTimeMillis()),
            obj.optLong("interval", 0),
            obj.optBoolean("active", true)
        );
    }
}
