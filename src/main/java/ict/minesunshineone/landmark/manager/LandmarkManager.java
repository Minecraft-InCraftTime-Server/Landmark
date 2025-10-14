package ict.minesunshineone.landmark.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.model.Landmark;

public class LandmarkManager {

    private final LandmarkPlugin plugin;
    private final Map<String, Landmark> landmarks;
    private final Map<UUID, Set<String>> unlockedLandmarks;
    private final Map<UUID, Long> cooldowns;
    private static final int MAX_MENU_COLUMNS = 7;

    public LandmarkManager(LandmarkPlugin plugin) {
        this.plugin = plugin;
        this.landmarks = new HashMap<>();
        this.unlockedLandmarks = new HashMap<>();
        this.cooldowns = new HashMap<>();
        loadData();
    }

    public void createLandmark(String name, Location location, String description) {
        if (location == null || location.getWorld() == null) {
            plugin.getSLF4JLogger().error("无法创建锚点：位置或世界为空");
            return;
        }

        if (landmarks.containsKey(name.toLowerCase())) {
            return;
        }

        try {
            // 计算新锚点的菜单位置
            int[] menuPosition = calculateNextMenuPosition();

            // 创建锚点并保存
            Landmark landmark = new Landmark(name, location, description, menuPosition[0], menuPosition[1]);
            landmarks.put(name.toLowerCase(), landmark);

            plugin.getSLF4JLogger().info("成功创建锚点展示实体: {}", name);
            saveData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getSLF4JLogger().error("创建锚点时发生错误: {}", e.getMessage());
            landmarks.remove(name.toLowerCase());
        }
    }

    private int[] calculateNextMenuPosition() {
        int maxRow = getMaxMenuRows();
        boolean[][] occupied = new boolean[maxRow + 1][MAX_MENU_COLUMNS + 1];

        for (Landmark landmark : landmarks.values()) {
            int row = landmark.getMenuRow();
            int col = landmark.getMenuColumn();
            if (row >= 1 && row <= maxRow && col >= 1 && col <= MAX_MENU_COLUMNS) {
                occupied[row][col] = true;
            }
        }

        for (int row = 1; row <= maxRow; row++) {
            for (int col = 1; col <= MAX_MENU_COLUMNS; col++) {
                if (!occupied[row][col]) {
                    return new int[]{row, col};
                }
            }
        }

        plugin.getSLF4JLogger().warn("锚点菜单已满，使用默认位置 (1,1)");
        return new int[]{1, 1};
    }

    public void deleteLandmark(String name) {
        String lowerName = name.toLowerCase();
        Landmark landmark = landmarks.get(lowerName);
        if (landmark != null) {
            // 移除锚点数据
            landmarks.remove(lowerName);

            // 从所有玩家的解锁列表中移除
            for (Map.Entry<UUID, Set<String>> entry : unlockedLandmarks.entrySet()) {
                entry.getValue().remove(lowerName);
            }

            // 保存更新后的数据
            saveData();

            // 保存所有受影响的玩家数据
            for (UUID playerId : unlockedLandmarks.keySet()) {
                savePlayerData(playerId);
            }
        }
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
        // 如果玩家有无视冷却权限，直接返回true
        if (player.hasPermission("landmark.bypass.cooldown")) {
            return true;
        }
        long lastTeleport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownTime = plugin.getConfigManager().getCooldownTime() * 1000L;
        return System.currentTimeMillis() - lastTeleport >= cooldownTime;
    }

    public void teleport(Player player, String landmarkName) {
        // 规范化名称处理
        String normalizedName = landmarkName.toLowerCase().trim();

        // 检查锚点是否存在
        Landmark targetLandmark = landmarks.get(normalizedName);
        if (targetLandmark == null) {
            plugin.getConfigManager().sendMessage(player, "landmark-not-exist",
                    "<red>该锚点不存在！</red>");
            return;
        }

        // 检查是否已解锁
        if (!isLandmarkUnlocked(player, normalizedName)) {
            plugin.getConfigManager().sendMessage(player, "landmark-not-unlocked",
                    "<red>你需要先解锁该锚点！</red>");
            return;
        }

        // 检查玩家是否在任意已解锁锚点围内
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
                    "<red>传送中，还需等待 <time> 秒</red>",
                    "<time>", String.valueOf(Math.max(0, remainingTime)));
            return;
        }

        // 执行传送
        Location targetLocation = targetLandmark.getLocation();
        Location playerLocation = player.getLocation();

        // 先在玩家当前位置的区域移除乘客
        plugin.getServer().getRegionScheduler().execute(plugin, playerLocation, () -> {
            if (!player.getPassengers().isEmpty()) {
                for (Entity passenger : player.getPassengers()) {
                    player.removePassenger(passenger);
                }
            }

            // 然后在目标位置的区域执行传送
            plugin.getServer().getRegionScheduler().execute(plugin, targetLocation, () -> {
                player.teleportAsync(targetLocation).thenAccept(result -> {
                    if (result) {
                        plugin.getConfigManager().sendMessage(player, "teleport-success", "",
                                "<landmark>", targetLandmark.getName());
                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    } else {
                        plugin.getConfigManager().sendMessage(player, "teleport-failed", "");
                    }
                });
            });
        });
    }

    public final void loadData() {
        File dataFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDataFileName());
        if (dataFile.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection landmarksSection = data.getConfigurationSection("landmarks");
            if (landmarksSection != null) {
                for (String key : landmarksSection.getKeys(false)) {
                    ConfigurationSection landmarkSection = landmarksSection.getConfigurationSection(key);
                    if (landmarkSection != null) {
                        try {
                            Location location = landmarkSection.getLocation("location");
                            if (location != null && location.getWorld() != null) {
                                String description = landmarkSection.getString("description", "暂无描述");
                                // 修改默认值：行从1开始，列从1开始
                                int menuRow = landmarkSection.getInt("menu_row", 1);
                                int menuColumn = landmarkSection.getInt("menu_column", 1);

                                int maxRow = getMaxMenuRows();
                                if (menuRow < 1 || menuRow > maxRow) {
                                    plugin.getSLF4JLogger().warn("锚点 {} 的菜单行 {} 超出范围，已调整至有效值", key, menuRow);
                                    menuRow = Math.max(1, Math.min(menuRow, maxRow));
                                }
                                if (menuColumn < 1 || menuColumn > MAX_MENU_COLUMNS) {
                                    plugin.getSLF4JLogger().warn("锚点 {} 的菜单列 {} 超出范围，已调整至有效值", key, menuColumn);
                                    menuColumn = Math.max(1, Math.min(menuColumn, MAX_MENU_COLUMNS));
                                }

                                Landmark landmark = new Landmark(key, location, description, menuRow, menuColumn);
                                landmarks.put(key.toLowerCase(), landmark);
                            }
                        } catch (IllegalArgumentException | IllegalStateException e) {
                            plugin.getSLF4JLogger().error("加载锚点 {} 时发生错误: {}", key, e.getMessage());
                        }
                    }
                }

            }
        }

        // 加载玩家数据
        loadPlayerData();
    }

    private void loadPlayerData() {
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
            landmarkSection.set("menu_row", landmark.getMenuRow());
            landmarkSection.set("menu_column", landmark.getMenuColumn());
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
        String lowerOldName = oldName.toLowerCase();
        String lowerNewName = newName.toLowerCase();

        // 检查新名称是否已存在
        if (landmarks.containsKey(lowerNewName)) {
            return;
        }

        Landmark landmark = landmarks.remove(lowerOldName);
        if (landmark != null) {
            // 保持原有的菜单位置
            int oldRow = landmark.getMenuRow();
            int oldColumn = landmark.getMenuColumn();

            landmark.setName(newName);
            landmarks.put(lowerNewName, landmark);

            // 更新所有玩家的解锁列表
            for (Set<String> unlockedSet : unlockedLandmarks.values()) {
                if (unlockedSet.remove(lowerOldName)) {
                    unlockedSet.add(lowerNewName);
                }
            }

            // 确保位置信息不变
            landmark.setMenuRow(oldRow);
            landmark.setMenuColumn(oldColumn);

            // 保存更新后的数据
            saveData();

            // 保存所有受影响的玩家数据
            for (UUID playerId : unlockedLandmarks.keySet()) {
                savePlayerData(playerId);
            }
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

    public void cleanupPlayerData(UUID playerId) {
        unlockedLandmarks.remove(playerId);
        cooldowns.remove(playerId);

        // 删除玩家数据文件
        File playerFile = new File(new File(plugin.getDataFolder(), "player_data"), playerId.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }

    public boolean isPlayerNearLandmark(Player player, Location landmarkLoc) {
        Location playerLoc = player.getLocation();
        return playerLoc.getWorld() != null
                && playerLoc.getWorld().equals(landmarkLoc.getWorld())
                && playerLoc.distance(landmarkLoc) <= plugin.getConfigManager().getUnlockRadius();
    }

    public void cleanup() {
        // 清理数据结构
        landmarks.clear();
        unlockedLandmarks.clear();
        cooldowns.clear();
    }

    public void updateMenuPosition(String landmarkName, int newRow, int newColumn) {
        Landmark landmark = landmarks.get(landmarkName.toLowerCase());
        if (landmark != null) {
            int maxRow = getMaxMenuRows();
            // 确保位置在有效范围内（第2-倒数第二行，即索引1-maxRow，列1-7）
            if (newRow >= 1 && newRow <= maxRow && newColumn >= 1 && newColumn <= MAX_MENU_COLUMNS) {
                landmark.setMenuRow(newRow);
                landmark.setMenuColumn(newColumn);
                saveData();
            }
        }
    }

    private int getMaxMenuRows() {
        int guiSize = plugin.getConfigManager().getConfig().getInt("gui.size", 54);
        int totalRows = Math.max(1, guiSize / 9);
        return Math.max(1, totalRows - 2);
    }

    // 新增一键解锁所有锚点的方法
    public void unlockAllLandmarks(Player player) {
        if (!player.hasPermission("landmark.unlock.all")) {
            return;
        }
        Set<String> playerUnlocked = unlockedLandmarks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        for (String landmarkName : landmarks.keySet()) {
            playerUnlocked.add(landmarkName.toLowerCase());
        }
        savePlayerData(player.getUniqueId());
        plugin.getConfigManager().sendMessage(player, "unlock-all-success", "<gradient:green:aqua>✧ 魔法师解开了所有锚点的封印!</gradient>");
    }
}
