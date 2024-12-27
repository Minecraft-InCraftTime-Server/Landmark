package com.example.landmark.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.gui.LandmarkMenu;
import com.example.landmark.model.Landmark;

import net.kyori.adventure.title.Title;

public class PlayerListener implements Listener {

    private final LandmarkPlugin plugin;
    private final Map<UUID, Long> lastCheckTimes = new HashMap<>();
    private static final long CHECK_INTERVAL = 500L; // 500ms检查间隔
    private final Map<String, List<Location>> particleLocationsCache = new HashMap<>();
    private final Map<String, Long> lastCacheUpdateTime = new HashMap<>();

    public PlayerListener(LandmarkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        LandmarkMenu.handleClick(event);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        long currentTime = System.currentTimeMillis();

        // 限制检查频率
        if (currentTime - lastCheckTimes.getOrDefault(player.getUniqueId(), 0L) < CHECK_INTERVAL) {
            return;
        }

        lastCheckTimes.put(player.getUniqueId(), currentTime);
        checkLandmarkUnlock(player, event.getTo());
    }

    private void checkLandmarkUnlock(Player player, Location playerLoc) {
        // 如果玩家是观察者模式，直接返回
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        for (Map.Entry<String, Landmark> entry : plugin.getLandmarkManager().getLandmarks().entrySet()) {
            String landmarkName = entry.getKey();
            Landmark landmark = entry.getValue();
            Location landmarkLoc = landmark.getLocation();

            // 使用 LandmarkManager 中的辅助方法检查距离
            if (!plugin.getLandmarkManager().isLandmarkUnlocked(player, landmarkName)
                    && plugin.getLandmarkManager().isPlayerNearLandmark(player, landmarkLoc)) {

                unlockLandmark(player, landmark);
            }

            // 优化粒子效果显示范围检查
            if (playerLoc.getWorld().equals(landmarkLoc.getWorld())
                    && playerLoc.distance(landmarkLoc) <= 20) {
                spawnLandmarkParticles(player, landmarkLoc);
            }
        }
    }

    private void unlockLandmark(Player player, Landmark landmark) {
        // 解锁锚点
        plugin.getLandmarkManager().unlockLandmark(player, landmark.getName());

        // 播放音效和显示消息
        playUnlockSound(player);
        showUnlockEffects(player, landmark);
    }

    private void showUnlockEffects(Player player, Landmark landmark) {
        // 只给解锁的玩家发送标题
        Title title = Title.title(
                plugin.getConfigManager().getMessage("unlock-title", ""),
                plugin.getConfigManager().getMessage("unlock-subtitle", "")
                        .replaceText(builder -> builder.match("<landmark_name>")
                        .replacement(landmark.getName()))
        );
        player.showTitle(title);

        // 只给解锁的玩家发送消息
        plugin.getConfigManager().sendMessage(player, "unlock-message",
                "<green>你已解锁锚点: <gold><landmark_name></gold></green>",
                "<landmark_name>", landmark.getName());
    }

    // 优化粒子效果显示
    private void spawnLandmarkParticles(Player player, Location landmarkLoc) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        long currentTime = System.currentTimeMillis();

        // 中心点动画效果
        if (config.getBoolean("particles.center.animation.enabled", true)) {
            double heightRange = config.getDouble("particles.center.animation.height-range", 2.0);
            double animSpeed = config.getDouble("particles.center.animation.speed", 0.5);
            double yOffset = config.getDouble("particles.center.y-offset", 1.0);

            // 使用正弦函数创建上下浮动效果
            double yAnimation = Math.sin((currentTime / 1000.0) * animSpeed) * heightRange / 2;

            try {
                Particle centerParticle = Particle.valueOf(config.getString("particles.center.type", "END_ROD"));
                Location animatedLoc = landmarkLoc.clone().add(0, yOffset + yAnimation, 0);

                player.spawnParticle(
                        centerParticle,
                        animatedLoc,
                        config.getInt("particles.center.count", 2),
                        config.getDouble("particles.center.offset", 0.1),
                        config.getDouble("particles.center.offset", 0.1),
                        config.getDouble("particles.center.offset", 0.1),
                        config.getDouble("particles.center.speed", 0.01)
                );
            } catch (IllegalArgumentException e) {
                plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.center.type"));
            }
        }

        // 边界效果
        if (config.getBoolean("particles.border.animation.enabled", true)) {
            try {
                Particle borderParticle = Particle.valueOf(config.getString("particles.border.type", "DRAGON_BREATH"));
                List<Location> baseLocations = getParticleLocations(landmarkLoc);
                double height = config.getDouble("particles.border.animation.height", 3.0);
                double density = config.getDouble("particles.border.animation.density", 0.5);
                int displayRange = config.getInt("particles.border.display-range", 32);

                // 只为在显示范围内的位置生成粒子
                for (Location baseLoc : baseLocations) {
                    if (player.getLocation().distance(baseLoc) <= displayRange) {
                        // 创建垂直粒子柱
                        for (double y = 0; y <= height; y += density) {
                            // 使用正弦函数创建波浪效果
                            double waveOffset = Math.sin((currentTime / 1000.0) + (baseLoc.getX() + baseLoc.getZ()) * 0.5) * 0.2;
                            Location particleLoc = baseLoc.clone().add(0, y + waveOffset, 0);

                            player.spawnParticle(
                                    borderParticle,
                                    particleLoc,
                                    config.getInt("particles.border.count", 1),
                                    config.getDouble("particles.border.offset", 0),
                                    config.getDouble("particles.border.offset", 0),
                                    config.getDouble("particles.border.offset", 0),
                                    config.getDouble("particles.border.speed", 0)
                            );
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.border.type"));
            }
        }
    }

    private List<Location> getParticleLocations(Location landmarkLoc) {
        String locationKey = landmarkLoc.getWorld().getName() + ","
                + landmarkLoc.getBlockX() + ","
                + landmarkLoc.getBlockY() + ","
                + landmarkLoc.getBlockZ();

        long currentTime = System.currentTimeMillis();
        long cacheInterval = plugin.getConfigManager().getConfig()
                .getLong("particles.border.update-interval", 5000);

        if (!particleLocationsCache.containsKey(locationKey)
                || currentTime - lastCacheUpdateTime.getOrDefault(locationKey, 0L) > cacheInterval) {
            int points = plugin.getConfigManager().getConfig()
                    .getInt("particles.border.points", 8);
            List<Location> locations = calculateParticleLocations(landmarkLoc, points);
            particleLocationsCache.put(locationKey, locations);
            lastCacheUpdateTime.put(locationKey, currentTime);
            return locations;
        }

        return particleLocationsCache.get(locationKey);
    }

    private List<Location> calculateParticleLocations(Location landmarkLoc, int points) {
        List<Location> locations = new ArrayList<>();
        double radius = plugin.getConfigManager().getUnlockRadius();
        double angleIncrement = Math.PI * 2 / points;

        for (double t = 0; t < Math.PI * 2; t += angleIncrement) {
            double x = landmarkLoc.getX() + radius * Math.cos(t);
            double z = landmarkLoc.getZ() + radius * Math.sin(t);
            locations.add(new Location(landmarkLoc.getWorld(), x, landmarkLoc.getY(), z));
        }

        return locations;
    }

    private void playUnlockSound(Player player) {
        String soundName = plugin.getConfigManager().getUnlockSound().toString();
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(),
                    sound,
                    plugin.getConfigManager().getUnlockSoundVolume(),
                    plugin.getConfigManager().getUnlockSoundPitch());
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().error("无效的声音设置: {}", soundName);
        }
    }
}
