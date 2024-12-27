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
    private static final long CACHE_DURATION = 5000L; // 缓存5秒

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
        String locationKey = landmarkLoc.getWorld().getName() + ","
                + landmarkLoc.getBlockX() + ","
                + landmarkLoc.getBlockY() + ","
                + landmarkLoc.getBlockZ();

        long currentTime = System.currentTimeMillis();
        List<Location> particleLocations;

        // 检查缓存是否有效
        if (!particleLocationsCache.containsKey(locationKey)
                || currentTime - lastCacheUpdateTime.getOrDefault(locationKey, 0L) > CACHE_DURATION) {
            // 计算新的粒子位置
            particleLocations = calculateParticleLocations(landmarkLoc);
            particleLocationsCache.put(locationKey, particleLocations);
            lastCacheUpdateTime.put(locationKey, currentTime);
        } else {
            particleLocations = particleLocationsCache.get(locationKey);
        }

        // 显示粒子效果
        for (Location particleLoc : particleLocations) {
            if (player.getLocation().distance(particleLoc) <= 32) {
                player.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        // 中心点效果
        player.spawnParticle(Particle.END_ROD, landmarkLoc.clone().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.01);
    }

    private List<Location> calculateParticleLocations(Location landmarkLoc) {
        List<Location> locations = new ArrayList<>();
        double radius = plugin.getConfigManager().getUnlockRadius();
        int particleCount = 8;
        double angleIncrement = Math.PI * 2 / particleCount;

        for (double t = 0; t < Math.PI * 2; t += angleIncrement) {
            double x = landmarkLoc.getX() + radius * Math.cos(t);
            double z = landmarkLoc.getZ() + radius * Math.sin(t);
            Location baseLoc = new Location(landmarkLoc.getWorld(), x, landmarkLoc.getY() + 0.1, z);
            locations.add(baseLoc);

            // 添加上升效果的位置
            for (double y = 0; y < 2; y += 1.0) {
                locations.add(baseLoc.clone().add(0, y, 0));
            }
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
