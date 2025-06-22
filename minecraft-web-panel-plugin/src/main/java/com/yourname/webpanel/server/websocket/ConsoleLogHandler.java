package com.yourname.webpanel.server.websocket;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

// Konsol çıktısını WebSocket ile web arayüzüne gönderen handler
public class ConsoleLogHandler extends Handler {
    @Override
    public void publish(LogRecord record) {
        if (record == null || record.getMessage() == null) return;
        String message = getFormatter() != null ? getFormatter().format(record) : record.getMessage();
        String level = record.getLevel().getName().toLowerCase();
        WebSocketHandler.sendConsoleOutput(message, level);
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
}
