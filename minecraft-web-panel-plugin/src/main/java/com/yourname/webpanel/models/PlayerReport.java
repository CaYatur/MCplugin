package com.yourname.webpanel.models;

import org.json.JSONObject;

public class PlayerReport {
    public String reporter;
    public String reported;
    public String reason;
    public long time;
    public boolean resolved;

    public PlayerReport(String reporter, String reported, String reason, long time, boolean resolved) {
        this.reporter = reporter;
        this.reported = reported;
        this.reason = reason;
        this.time = time;
        this.resolved = resolved;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("reporter", reporter);
        obj.put("reported", reported);
        obj.put("reason", reason);
        obj.put("time", time);
        obj.put("resolved", resolved);
        return obj;
    }

    public static PlayerReport fromJson(JSONObject obj) {
        return new PlayerReport(
            obj.optString("reporter"),
            obj.optString("reported"),
            obj.optString("reason"),
            obj.optLong("time", System.currentTimeMillis()),
            obj.optBoolean("resolved", false)
        );
    }
}
