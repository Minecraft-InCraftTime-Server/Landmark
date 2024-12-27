package com.example.landmark.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.model.Landmark;

public class LandmarkManager {

    private final LandmarkPlugin plugin;
    private final Map<String, Landmark> landmarks;
    private final Map<UUID, Set<String>> unlockedLandmarks;
    private final Map<UUID, Long> cooldowns;

    public LandmarkManager(LandmarkPlugin plugin) {
        this.plugin = plugin;
        this.landmarks = new HashMap<>();
        this.unlockedLandmarks = new HashMap<>();
        this.cooldowns = new HashMap<>();
        loadData();
    }

    public void createLandmark(String name, Location location, String description) {
        Landmark landmark = new Landmark(name, location, description);
        landmarks.put(name.toLowerCase(), landmark);
        saveData();
    }

    public void deleteLandmark(String name) {
        String lowerName = name.toLowerCase();
        landmarks.remove(lowerName);

        // 从所有玩家的解锁列表中移除并保存
        for (Map.Entry<UUID, Set<String>> entry : unlockedLandmarks.entrySet()) {
            if (entry.getValue().remove(lowerName)) {
                savePlayerData(entry.getKey());
            }
        }

        // 保存锚点数据
        saveLandmarkData();
    }

    public boolean isLandmarkUnlocked(Player player, String landmarkName) {
        Set<String> playerUnlocked = unlockedLandmarks.getOrDefault(player.getUniqueId(), new HashSet<>());
        return playerUnlocked.contains(landmarkName.toLowerCase());
    }

    public void unlockLandmark(Player player, String landmarkName) {
        unlockedLandmarks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                .add(landmarkName.toLowerCase());
        savePlayerData(player.getUniqueId()); // 只保存变更的玩家数据
    }

    public boolean canTeleport(Player player) {
        long lastTeleport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownTime = plugin.getConfigManager().getCooldownTime() * 1000L;
        return System.currentTimeMillis() - lastTeleport >= cooldownTime;
    }

    public void teleport(Player player, String landmarkName) {
        Landmark targetLandmark = landmarks.get(landmarkName.toLowerCase());
        if (targetLandmark == null) {
            plugin.getConfigManager().sendMessage(player, "landmark-not-exist", "");
            return;
        }

        if (!isLandmarkUnlocked(player, landmarkName)) {
            plugin.getConfigManager().sendMessage(player, "landmark-not-unlocked", "");
            return;
        }

        // 检查玩家是否在任意已解锁锚点范围内
        boolean nearUnlockedLandmark = false;
        Location playerLoc = player.getLocation();

        for (Map.Entry<String, Landmark> entry : landmarks.entrySet()) {
            Landmark landmark = entry.getValue();
            if (isLandmarkUnlocked(player, entry.getKey())
                    && playerLoc.getWorld().equals(landmark.getLocation().getWorld())
                    && playerLoc.distance(landmark.getLocation()) <= plugin.getConfigManager().getUnlockRadius()) {
                nearUnlockedLandmark = true;
                break;
            }
        }

        if (!nearUnlockedLandmark) {
            plugin.getConfigManager().sendMessage(player, "not-at-landmark", "");
            return;
        }

        if (!canTeleport(player)) {
            long remainingTime = (cooldowns.getOrDefault(player.getUniqueId(), System.currentTimeMillis())
                    + (plugin.getConfigManager().getCooldownTime() * 1000L)
                    - System.currentTimeMillis()) / 1000;
            plugin.getConfigManager().sendMessage(player, "teleport-cooldown",
                    "<red>传送冷却中，还需等待 <time> 秒</red>",
                    "<time>", String.valueOf(Math.max(0, remainingTime)));
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            player.teleportAsync(targetLandmark.getLocation()).thenAccept(result -> {
                if (result) {
                    plugin.getConfigManager().sendMessage(player, "teleport-success", "",
                            "<landmark>", targetLandmark.getName());
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                } else {
                    plugin.getConfigManager().sendMessage(player, "teleport-failed", "");
                }
            });
        });
    }

    public final void loadData() {
        // 加载锚点数据
        File dataFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDataFileName());
        if (dataFile.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection landmarksSection = data.getConfigurationSection("landmarks");
            if (landmarksSection != null) {
                for (String key : landmarksSection.getKeys(false)) {
                    ConfigurationSection landmarkSection = landmarksSection.getConfigurationSection(key);
                    if (landmarkSection != null) {
                        Location location = landmarkSection.getLocation("location");
                        String description = landmarkSection.getString("description", "暂无描述");
                        landmarks.put(key.toLowerCase(), new Landmark(key, location, description));
                    }
                }
            }
        }

        // 加载玩家数据
        File playerDataFolder = new File(plugin.getDataFolder(), "player_data");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                String uuid = playerFile.getName().replace(".yml", "");
                YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                List<String> unlockedList = playerData.getStringList("unlocked_landmarks");
                unlockedLandmarks.put(UUID.fromString(uuid), new HashSet<>(unlockedList));
            }
        }
    }

    public void saveData() {
        // 保存锚点数据
        File dataFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDataFileName());
        YamlConfiguration data = new YamlConfiguration();
        ConfigurationSection landmarksSection = data.createSection("landmarks");
        for (Map.Entry<String, Landmark> entry : landmarks.entrySet()) {
            ConfigurationSection landmarkSection = landmarksSection.createSection(entry.getKey());
            Landmark landmark = entry.getValue();
            landmarkSection.set("location", landmark.getLocation());
            landmarkSection.set("description", landmark.getDescription());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("无法保存锚点数据: {}", e.getMessage());
        }

        // 保存玩家数据
        File playerDataFolder = new File(plugin.getDataFolder(), "player_data");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        for (Map.Entry<UUID, Set<String>> entry : unlockedLandmarks.entrySet()) {
            File playerFile = new File(playerDataFolder, entry.getKey().toString() + ".yml");
            YamlConfiguration playerData = new YamlConfiguration();
            playerData.set("unlocked_landmarks", new ArrayList<>(entry.getValue()));
            try {
                playerData.save(playerFile);
            } catch (IOException e) {
                plugin.getSLF4JLogger().error("无法保存玩家数据 {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    public void editLandmarkDescription(String name, String newDescription) {
        Landmark landmark = landmarks.get(name.toLowerCase());
        if (landmark != null) {
            landmark.setDescription(newDescription);
            saveData();
        }
    }

    public void renameLandmark(String oldName, String newName) {
        Landmark landmark = landmarks.remove(oldName.toLowerCase());
        if (landmark != null) {
            landmark.setName(newName);
            landmarks.put(newName.toLowerCase(), landmark);

            // 更新所有玩家的解锁列表
            for (Set<String> unlockedSet : unlockedLandmarks.values()) {
                if (unlockedSet.remove(oldName.toLowerCase())) {
                    unlockedSet.add(newName.toLowerCase());
                }
            }
            saveData();
        }
    }

    public Map<String, Landmark> getLandmarks() {
        return landmarks;
    }

    public Set<String> getUnlockedLandmarks(Player player) {
        return unlockedLandmarks.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public void cleanupCooldowns() {
        long currentTime = System.currentTimeMillis();
        long cooldownTime = plugin.getConfigManager().getCooldownTime() * 1000L;
        cooldowns.entrySet().removeIf(entry
                -> currentTime - entry.getValue() >= cooldownTime);
    }

    private void savePlayerData(UUID playerId) {
        File playerFile = new File(new File(plugin.getDataFolder(), "player_data"), playerId.toString() + ".yml");
        YamlConfiguration playerData = new YamlConfiguration();
        playerData.set("unlocked_landmarks", new ArrayList<>(unlockedLandmarks.getOrDefault(playerId, new HashSet<>())));
        try {
            playerData.save(playerFile);
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("无法保存玩家数据 {}: {}", playerId, e.getMessage());
        }
    }

    private void saveLandmarkData() {
        File dataFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDataFileName());
        File backupFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDataFileName() + ".backup");

        // 创建备份
        if (dataFile.exists()) {
            try {
                Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getSLF4JLogger().error("无法创建数据备份: {}", e.getMessage());
            }
        }

        YamlConfiguration data = new YamlConfiguration();
        ConfigurationSection landmarksSection = data.createSection("landmarks");

        landmarks.forEach((name, landmark) -> {
            ConfigurationSection section = landmarksSection.createSection(name);
            section.set("location", landmark.getLocation());
            section.set("description", landmark.getDescription());
        });

        try {
            data.save(dataFile);
            // 保存成功后删除备份
            if (backupFile.exists()) {
                backupFile.delete();
            }
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("无法保存锚点数据: {}", e.getMessage());
            // 恢复备份
            if (backupFile.exists()) {
                try {
                    Files.copy(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getSLF4JLogger().info("已恢复数据备份");
                } catch (IOException ex) {
                    plugin.getSLF4JLogger().error("无法恢复数据备份: {}", ex.getMessage());
                }
            }
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        unlockedLandmarks.remove(playerId);
        cooldowns.remove(playerId);

        // 删除玩家数据文件
        File playerFile = new File(new File(plugin.getDataFolder(), "player_data"), playerId.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }

    public void cleanupInactivePlayers(long inactiveTime) {
        long currentTime = System.currentTimeMillis();
        int cleanedCount = 0;

        for (Iterator<Map.Entry<UUID, Set<String>>> it = unlockedLandmarks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Set<String>> entry = it.next();
            UUID playerId = entry.getKey();

            if (Bukkit.getPlayer(playerId) == null
                    && currentTime - cooldowns.getOrDefault(playerId, currentTime) > inactiveTime) {
                cleanupPlayerData(playerId);
                it.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            plugin.getSLF4JLogger().info("已清理 {} 个不活跃玩家的数据", cleanedCount);
        }
    }

    private boolean isWorldValid(Location location) {
        return location != null && location.getWorld() != null;
    }

    public boolean isPlayerNearLandmark(Player player, Location landmarkLoc) {
        Location playerLoc = player.getLocation();
        return playerLoc.getWorld() != null
                && playerLoc.getWorld().equals(landmarkLoc.getWorld())
                && playerLoc.distance(landmarkLoc) <= plugin.getConfigManager().getUnlockRadius();
    }

    // 其他方法...
}
