package com.yourname.webpanel.listeners;

import com.yourname.webpanel.server.websocket.WebSocketHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class AntiCheatListener implements Listener {
    private final JavaPlugin plugin;
    private final Map<String, Long> lastMove = new HashMap<>();
    private final Map<String, Integer> fastBreakCount = new HashMap<>();

    // KillAura gelişmiş tespit için saldırı logu
    private final Map<String, Long> lastAttackTime = new HashMap<>();
    private final Map<String, Integer> attackCount = new HashMap<>();
    private final Map<String, Long> lastYaw = new HashMap<>();
    private final Map<String, Long> lastPitch = new HashMap<>();
    private final Map<String, Long> lastJumpTime = new HashMap<>();
    private final Map<String, Integer> jumpCount = new HashMap<>();
    private final Map<String, Long> lastCommandTime = new HashMap<>();
    private final Map<String, Integer> commandSpamCount = new HashMap<>();
    private final Map<String, Long> lastDropTime = new HashMap<>();
    private final Map<String, Integer> dropSpamCount = new HashMap<>();

    private static boolean detectSpeed = true;
    private static boolean detectFly = true;
    private static boolean detectFastBreak = true;
    private static boolean detectKillAura = true;
    private static boolean detectWaterWalk = true;

    public static void handleCheatSettings(org.json.JSONObject settings) {
        detectSpeed = settings.optBoolean("speed", true);
        detectFly = settings.optBoolean("fly", true);
        detectFastBreak = settings.optBoolean("fastbreak", true);
        detectKillAura = settings.optBoolean("killaura", true);
        detectWaterWalk = settings.optBoolean("waterwalk", true);
    }

    public AntiCheatListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) return;
        double dist = event.getFrom().distance(event.getTo());
        // Speed: block collision, potion effect, velocity
        boolean hasSpeed = p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        double maxDist = hasSpeed ? 1.2 : 0.7;
        if (detectSpeed && dist > maxDist) {
            sendCheatAlert(p.getName(), "Şüpheli hız tespit edildi! (Speed hack?) Mesafe: " + dist, "speed");
        }
        // Fly: Y ekseninde uzun süreli sabit kalma, velocity, fall distance
        double fromY = event.getFrom().getY();
        double toY = event.getTo().getY();
        if (detectFly && !p.isOnGround() && p.getVelocity().getY() == 0 && p.getFallDistance() == 0 && Math.abs(toY - fromY) < 0.01) {
            sendCheatAlert(p.getName(), "Havada uzun süre sabit kalma tespit edildi! (Fly hack?)", "fly");
        }
        // Water walk tespiti (basit): suyun üstünde yürüyorsa
        if (detectWaterWalk && p.getLocation().getBlock().getType() == Material.WATER && p.getLocation().add(0, -1, 0).getBlock().getType() == Material.WATER) {
            sendCheatAlert(p.getName(), "Suyun üstünde yürüyüş tespit edildi!", "waterwalk");
        }
        // Hızlı zıplama (bunny hop) tespiti: Y koordinatında ani artış
        if (toY - fromY > 0.3) { // Zıplama hareketi
            long now = System.currentTimeMillis();
            long last = lastJumpTime.getOrDefault(p.getName(), 0L);
            if (now - last < 300) {
                int cnt = jumpCount.getOrDefault(p.getName(), 0) + 1;
                jumpCount.put(p.getName(), cnt);
                if (cnt > 5) {
                    sendCheatAlert(p.getName(), "Çok hızlı zıplama tespit edildi! (Bunny hop/makro?)", "bunnyhop");
                    jumpCount.put(p.getName(), 0);
                }
            } else {
                jumpCount.put(p.getName(), 1);
            }
            lastJumpTime.put(p.getName(), now);
        }
        lastMove.put(p.getName(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) return;
        // Burada artık isJumping() kullanılmıyor, sadece diğer şüpheli davranışlar için kullanılabilir.
        // İstenirse başka şüpheli davranışlar buraya eklenebilir.
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!detectFastBreak) return;
        int count = fastBreakCount.getOrDefault(p.getName(), 0) + 1;
        fastBreakCount.put(p.getName(), count);
        if (count > 10) {
            sendCheatAlert(p.getName(), "Çok hızlı blok kırma tespit edildi!", "fastbreak");
            fastBreakCount.put(p.getName(), 0);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fastBreakCount.put(p.getName(), 0);
        }, 40L);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) return;
        long now = System.currentTimeMillis();
        long last = lastCommandTime.getOrDefault(p.getName(), 0L);
        if (now - last < 500) {
            int cnt = commandSpamCount.getOrDefault(p.getName(), 0) + 1;
            commandSpamCount.put(p.getName(), cnt);
            if (cnt > 5) {
                sendCheatAlert(p.getName(), "Çok hızlı komut kullanımı tespit edildi! (Spam/makro?)", "commandspam");
                commandSpamCount.put(p.getName(), 0);
            }
        } else {
            commandSpamCount.put(p.getName(), 1);
        }
        lastCommandTime.put(p.getName(), now);
        String[] args = event.getMessage().split(" ");
        if (args.length >= 2 && args[0].equalsIgnoreCase("/report")) {
            String reported = args[1];
            String reason = args.length > 2 ? event.getMessage().substring(args[0].length() + args[1].length() + 2) : "-";
            sendReport(event.getPlayer().getName(), reported, reason);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Raporun iletildi. Yönetici panelinden incelenecek.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) return;
        long now = System.currentTimeMillis();
        long last = lastDropTime.getOrDefault(p.getName(), 0L);
        if (now - last < 300) {
            int cnt = dropSpamCount.getOrDefault(p.getName(), 0) + 1;
            dropSpamCount.put(p.getName(), cnt);
            if (cnt > 10) {
                sendCheatAlert(p.getName(), "Çok hızlı eşya atma tespit edildi! (Makro?)", "itemspam");
                dropSpamCount.put(p.getName(), 0);
            }
        } else {
            dropSpamCount.put(p.getName(), 1);
        }
        lastDropTime.put(p.getName(), now);
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (attacker.isOp()) return;
        long now = System.currentTimeMillis();
        long last = lastAttackTime.getOrDefault(attacker.getName(), 0L);
        double reach = attacker.getLocation().distance(victim.getLocation());
        float yawDiff = Math.abs(attacker.getLocation().getYaw() - attacker.getEyeLocation().getYaw());
        float pitchDiff = Math.abs(attacker.getLocation().getPitch() - attacker.getEyeLocation().getPitch());
        // KillAura: Çok kısa sürede çok fazla saldırı, imkansız açı, reach, block arkasından vurma
        if (now - last < 250) {
            int cnt = attackCount.getOrDefault(attacker.getName(), 0) + 1;
            attackCount.put(attacker.getName(), cnt);
            if (cnt > 4) {
                sendCheatAlert(attacker.getName(), "Çok kısa sürede çok fazla saldırı (KillAura şüphesi)", "killaura");
                attackCount.put(attacker.getName(), 0);
            }
        } else {
            attackCount.put(attacker.getName(), 1);
        }
        lastAttackTime.put(attacker.getName(), now);
        // Reach kontrolü (3.1 bloktan fazla ise şüpheli)
        if (reach > 3.1) {
            sendCheatAlert(attacker.getName(), "Uzak mesafeden saldırı tespit edildi! (Reach/KillAura)", "killaura");
        }
        // Bakış açısı kontrolü (yaw/pitch farkı)
        double yawToVictim = Math.abs(attacker.getLocation().getYaw() - getYawTo(attacker, victim));
        if (yawToVictim > 60) {
            sendCheatAlert(attacker.getName(), "Bakış açısı hedefe uygun değil (KillAura/AutoAim)", "killaura");
        }
        // Block arkasından vurma (görüş hattı yoksa)
        if (!attacker.hasLineOfSight(victim)) {
            sendCheatAlert(attacker.getName(), "Block arkasından saldırı tespit edildi! (KillAura/WallHit)", "killaura");
        }
    }
    private double getYawTo(Player from, Player to) {
        double dx = to.getLocation().getX() - from.getLocation().getX();
        double dz = to.getLocation().getZ() - from.getLocation().getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return yaw < 0 ? yaw + 360 : yaw;
    }

    private void sendCheatAlert(String player, String message, String type) {
        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("type", "cheat_alert");
        obj.put("player", player);
        obj.put("message", message);
        obj.put("cheatType", type);
        obj.put("timestamp", System.currentTimeMillis());
        WebSocketHandler.broadcastToAll(obj);
    }

    private void sendReport(String reporter, String reported, String reason) {
        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("type", "player_report");
        obj.put("reporter", reporter);
        obj.put("reported", reported);
        obj.put("reason", reason);
        obj.put("timestamp", System.currentTimeMillis());
        WebSocketHandler.broadcastToAll(obj);
        // Backend'e de kaydet
        try {
            org.json.JSONObject reportObj = new org.json.JSONObject();
            reportObj.put("reporter", reporter);
            reportObj.put("reported", reported);
            reportObj.put("reason", reason);
            reportObj.put("time", System.currentTimeMillis());
            reportObj.put("resolved", false);
            org.json.JSONObject req = new org.json.JSONObject();
            req.put("action", "add");
            req.put("report", reportObj);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL("http://localhost:8080/api/reports").openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            try (java.io.OutputStream os = con.getOutputStream()) {
                os.write(req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            con.getInputStream().close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
