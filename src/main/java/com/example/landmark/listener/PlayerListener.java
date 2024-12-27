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
        double time = currentTime / 1000.0; // 用于动画计算

        // 中心点螺旋效果
        if (config.getBoolean("particles.center.animation.enabled", true)) {
            spawnCenterSpiral(player, landmarkLoc, config, time);
        }

        // 边界光柱效果
        if (config.getBoolean("particles.border.animation.enabled", true)) {
            spawnBorderPillars(player, landmarkLoc, config, time);
        }

        // 环绕效果
        if (config.getBoolean("particles.orbit.enabled", true)) {
            spawnOrbitParticles(player, landmarkLoc, config, time);
        }

        // 地面魔法阵
        if (config.getBoolean("particles.magic_circle.enabled", true)) {
            spawnMagicCircle(player, landmarkLoc, config, time);
        }
    }

    private void spawnCenterSpiral(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle centerParticle = Particle.valueOf(config.getString("particles.center.type", "END_ROD"));
            double spiralRadius = config.getDouble("particles.center.animation.spiral.radius", 1.0);
            double spiralSpeed = config.getDouble("particles.center.animation.spiral.speed", 2.0);
            double heightRange = config.getDouble("particles.center.animation.height-range", 2.0);
            double baseY = center.getY() + config.getDouble("particles.center.y-offset", 1.0);

            // 创建螺旋上升效果
            for (double y = 0; y < heightRange; y += 0.2) {
                double angle = y * 5 + time * spiralSpeed;
                double x = center.getX() + Math.cos(angle) * spiralRadius;
                double z = center.getZ() + Math.sin(angle) * spiralRadius;
                double yOffset = baseY + y + Math.sin(time * 2) * 0.2;

                player.spawnParticle(centerParticle, x, yOffset, z, 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.center.type"));
        }
    }

    private void spawnBorderPillars(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle borderParticle = Particle.valueOf(config.getString("particles.border.type", "DRAGON_BREATH"));
            List<Location> baseLocations = getParticleLocations(center);
            double height = config.getDouble("particles.border.animation.height", 3.0);
            double density = config.getDouble("particles.border.animation.density", 0.5);
            double waveAmplitude = config.getDouble("particles.border.animation.wave.amplitude", 0.3);
            double waveFrequency = config.getDouble("particles.border.animation.wave.frequency", 2.0);

            for (Location baseLoc : baseLocations) {
                if (player.getLocation().distance(baseLoc) <= config.getInt("particles.border.display-range", 32)) {
                    for (double y = 0; y <= height; y += density) {
                        double wave = Math.sin(time * waveFrequency + (baseLoc.getX() + baseLoc.getZ()) * 0.5) * waveAmplitude;
                        Location particleLoc = baseLoc.clone().add(0, y + wave, 0);
                        player.spawnParticle(borderParticle, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.border.type"));
        }
    }

    private void spawnOrbitParticles(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle orbitParticle = Particle.valueOf(config.getString("particles.orbit.type", "SOUL_FIRE_FLAME"));
            double radius = config.getDouble("particles.orbit.radius", 2.0);
            double speed = config.getDouble("particles.orbit.speed", 1.5);
            double height = config.getDouble("particles.orbit.height", 1.5);
            int particles = config.getInt("particles.orbit.particles", 3);

            for (int i = 0; i < particles; i++) {
                double angle = (2 * Math.PI * i / particles) + time * speed;
                double x = center.getX() + Math.cos(angle) * radius;
                double y = center.getY() + height + Math.sin(time * 2) * 0.3;
                double z = center.getZ() + Math.sin(angle) * radius;

                player.spawnParticle(orbitParticle, x, y, z, 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.orbit.type"));
        }
    }

    private void spawnMagicCircle(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle circleParticle = Particle.valueOf(config.getString("particles.magic_circle.type", "SPELL_WITCH"));
            int rings = config.getInt("particles.magic_circle.rings", 2);
            int points = config.getInt("particles.magic_circle.points", 20);
            double rotationSpeed = config.getDouble("particles.magic_circle.rotation_speed", 1.0);

            for (int ring = 1; ring <= rings; ring++) {
                double ringRadius = ring * 1.0;
                double ringOffset = time * rotationSpeed * (ring % 2 == 0 ? 1 : -1);

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i / points) + ringOffset;
                    double x = center.getX() + Math.cos(angle) * ringRadius;
                    double z = center.getZ() + Math.sin(angle) * ringRadius;

                    player.spawnParticle(circleParticle, x, center.getY() + 0.1, z, 1, 0, 0, 0, 0);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.magic_circle.type"));
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
