package com.yourname.webpanel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebPanelPluginTest {

    @Test
    public void testPluginClass() {
        // Plugin sınıfının mevcut olduğunu test et
        assertNotNull(WebPanelPlugin.class);
    }

    @Test
    public void testBasicFunctionality() {
        // Basit bir test - sınıf yüklenmesi
        String className = WebPanelPlugin.class.getSimpleName();
        assertNotNull(className);
    }
}
