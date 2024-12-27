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
        double time = System.currentTimeMillis() / 1000.0;

        // 六芒星魔法阵
        if (config.getBoolean("particles.center.enabled", true)) {
            spawnHexagramParticles(player, landmarkLoc, config, time);
        }

        // 边界光柱
        if (config.getBoolean("particles.border.enabled", true)) {
            spawnBorderPillars(player, landmarkLoc, config);
        }
    }

    private void spawnHexagramParticles(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle particle = Particle.valueOf(config.getString("particles.center.type", "END_ROD"));
            double radius = config.getDouble("particles.center.star_radius", 1.5);
            double height = config.getDouble("particles.center.height", 0.1);
            double rotationSpeed = config.getDouble("particles.center.rotation_speed", 0.8);
            int points = config.getInt("particles.center.points", 6);

            // 计算旋转角度
            double rotation = time * rotationSpeed;

            // 绘制第一个三角形
            for (int i = 0; i < points; i += 2) {
                double angle1 = (2 * Math.PI * i / points) + rotation;
                double angle2 = (2 * Math.PI * ((i + 2) % points) / points) + rotation;

                double x1 = center.getX() + Math.cos(angle1) * radius;
                double z1 = center.getZ() + Math.sin(angle1) * radius;
                double x2 = center.getX() + Math.cos(angle2) * radius;
                double z2 = center.getZ() + Math.sin(angle2) * radius;

                // 绘制线段
                double steps = 10;
                for (double j = 0; j <= steps; j++) {
                    double x = x1 + (x2 - x1) * (j / steps);
                    double z = z1 + (z2 - z1) * (j / steps);
                    player.spawnParticle(particle, x, center.getY() + height, z, 1, 0, 0, 0, 0);
                }
            }

            // 绘制第二个三角形（旋转30度）
            rotation += Math.PI / points;
            for (int i = 0; i < points; i += 2) {
                double angle1 = (2 * Math.PI * i / points) + rotation;
                double angle2 = (2 * Math.PI * ((i + 2) % points) / points) + rotation;

                double x1 = center.getX() + Math.cos(angle1) * radius;
                double z1 = center.getZ() + Math.sin(angle1) * radius;
                double x2 = center.getX() + Math.cos(angle2) * radius;
                double z2 = center.getZ() + Math.sin(angle2) * radius;

                // 绘制线段
                double steps = 10;
                for (double j = 0; j <= steps; j++) {
                    double x = x1 + (x2 - x1) * (j / steps);
                    double z = z1 + (z2 - z1) * (j / steps);
                    player.spawnParticle(particle, x, center.getY() + height, z, 1, 0, 0, 0, 0);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.center.type"));
        }
    }

    private void spawnBorderPillars(Player player, Location center, FileConfiguration config) {
        try {
            Particle particle = Particle.valueOf(config.getString("particles.border.type", "SPELL_WITCH"));
            List<Location> baseLocations = getParticleLocations(center);
            double height = config.getDouble("particles.border.height", 1.0);
            double density = config.getDouble("particles.border.density", 0.2);

            for (Location baseLoc : baseLocations) {
                if (player.getLocation().distance(baseLoc) <= config.getInt("particles.border.display-range", 32)) {
                    for (double y = 0; y <= height; y += density) {
                        player.spawnParticle(particle,
                                baseLoc.getX(),
                                center.getY() + y,
                                baseLoc.getZ(),
                                1, 0, 0, 0, 0);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.border.type"));
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
