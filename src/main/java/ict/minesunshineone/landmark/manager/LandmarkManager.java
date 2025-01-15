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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.model.Landmark;
import net.kyori.adventure.text.Component;

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

            // 使用统一的方法创建实体
            createLandmarkEntities(landmark);

            plugin.getSLF4JLogger().info("成功创建锚点展示实体: {}", name);
            saveData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getSLF4JLogger().error("创建锚点时发生错误: {}", e.getMessage());
            landmarks.remove(name.toLowerCase());
        }
    }

    private int[] calculateNextMenuPosition() {
        int maxRow = 1;  // 从第二行开始（索引1）
        int maxCol = 1;  // 从第一列开始（索引1）
        boolean[][] occupied = new boolean[4][9]; // 4行9列的网格，实际使用1-3行，1-7列

        // 标记已占用的位置
        for (Landmark landmark : landmarks.values()) {
            int row = landmark.getMenuRow();
            int col = landmark.getMenuColumn();
            if (row >= 1 && row <= 3 && col >= 1 && col <= 7) {  // 修改范围：第2-4行（索引1-3），第1-7列
                occupied[row][col] = true;
                maxRow = Math.max(maxRow, row);
                maxCol = Math.max(maxCol, col);
            }
        }

        // 寻找下一个可用位置（从第二行到第四行，第一列到第七列）
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                if (!occupied[row][col]) {
                    return new int[]{row, col};
                }
            }
        }

        // 如果当前行未满，添加到当前行的下一个位置
        if (maxCol < 7) {
            return new int[]{maxRow, maxCol + 1};
        }
        // 如果需要新的一行（最多到第3行，对应实际的第4行）
        if (maxRow < 3) {
            return new int[]{maxRow + 1, 1};  // 新行从第一列开始
        }
        // 如果菜单已满，返回默认位置（第1行第1列，对应实际的第2行第1列）
        return new int[]{1, 1};
    }

    public void deleteLandmark(String name) {
        String lowerName = name.toLowerCase();
        Landmark landmark = landmarks.get(lowerName);
        if (landmark != null) {
            // 移除交互实体
            if (landmark.getInteractionEntityId() != null) {
                Location loc = landmark.getLocation();
                if (loc != null && loc.getWorld() != null) {
                    // 移除交互实体
                    Entity entity = Bukkit.getEntity(landmark.getInteractionEntityId());
                    if (entity != null) {
                        entity.remove();
                    }

                    // 移除同位置的所有具有相同名称的交互实体
                    loc.getWorld().getNearbyEntities(loc, 2, 2, 2).stream()
                            .filter(e -> e instanceof Interaction)
                            .filter(e -> e.customName() != null && e.customName().equals(Component.text("§e[点击打开]")))
                            .forEach(Entity::remove);
                }
                landmark.setInteractionEntityId(null);
            }

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
        plugin.getServer().getRegionScheduler().execute(plugin, targetLocation, () -> {
            // 移除所有乘客
            if (!player.getPassengers().isEmpty()) {
                for (Entity passenger : player.getPassengers()) {
                    player.removePassenger(passenger);
                }
            }

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

                                Landmark landmark = new Landmark(key, location, description, menuRow, menuColumn);
                                String interactionId = landmarkSection.getString("interaction_entity_id");
                                if (interactionId != null) {
                                    landmark.setInteractionEntityId(UUID.fromString(interactionId));
                                }

                                landmarks.put(key.toLowerCase(), landmark);

                                // 延迟创建实体，确保世界加载完成
                                plugin.getServer().getRegionScheduler().runDelayed(plugin, location, task -> {
                                    if (location.getWorld() != null && location.getChunk().isLoaded()) {
                                        createLandmarkEntities(landmark);
                                    }
                                }, 100L);
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
            landmarkSection.set("interaction_entity_id", landmark.getInteractionEntityId() != null
                    ? landmark.getInteractionEntityId().toString() : null);
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

    private void createLandmarkEntities(Landmark landmark) {
        try {
            Location location = landmark.getLocation();
            if (location.getWorld() == null || !location.getChunk().isLoaded()) {
                return;
            }

            // 确保位置是方块中心
            Location centerLoc = location.clone();
            centerLoc.setX(location.getBlockX() + 0.5);
            centerLoc.setY(location.getBlockY());
            centerLoc.setZ(location.getBlockZ() + 0.5);

            // 创建交互实体
            Location interactLoc = centerLoc.clone().add(0, 0, 0);
            Interaction interaction = location.getWorld().spawn(interactLoc, Interaction.class, entity -> {
                entity.setInteractionWidth(3.5f);
                entity.setInteractionHeight(2.0f);
                entity.setPersistent(true);
                entity.setInvulnerable(true);
                entity.setCustomNameVisible(true);
                entity.customName(Component.text("§e[点击打开]"));
                entity.setGravity(false);
            });

            landmark.setInteractionEntityId(interaction.getUniqueId());
            saveData(); // 保存实体ID
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getSLF4JLogger().error("重建锚点实体时发生错误: {}", e.getMessage());
        }
    }

    public void cleanup() {
        // 清理所有实体
        for (Landmark landmark : landmarks.values()) {
            removeLandmarkEntities(landmark);
        }

        // 清理数据结构
        landmarks.clear();
        unlockedLandmarks.clear();
        cooldowns.clear();
    }

    private void removeLandmarkEntities(Landmark landmark) {
        if (landmark.getDisplayEntityId() != null) {
            Entity entity = Bukkit.getEntity(landmark.getDisplayEntityId());
            if (entity != null) {
                entity.remove();
            }
            landmark.setDisplayEntityId(null);
        }

        if (landmark.getInteractionEntityId() != null) {
            Entity entity = Bukkit.getEntity(landmark.getInteractionEntityId());
            if (entity != null) {
                entity.remove();
            }
            landmark.setInteractionEntityId(null);
        }
    }

    public void updateMenuPosition(String landmarkName, int newRow, int newColumn) {
        Landmark landmark = landmarks.get(landmarkName.toLowerCase());
        if (landmark != null) {
            // 确保位置在有效范围内（第2-4行，即索引1-3，列1-7）
            if (newRow >= 1 && newRow <= 3 && newColumn >= 1 && newColumn <= 7) {
                landmark.setMenuRow(newRow);
                landmark.setMenuColumn(newColumn);
                saveData();
            }
        }
    }
}
