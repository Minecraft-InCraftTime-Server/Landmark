package ict.minesunshineone.landmark.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
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
    private static final int MENU_COLUMNS = 7;
    private final Random random = new Random();
    
    // 精选的美观物品列表
    private static final List<Material> ICON_MATERIALS = Arrays.asList(
        // 宝石类
        Material.DIAMOND, Material.EMERALD, Material.AMETHYST_SHARD, Material.PRISMARINE_SHARD,
        Material.ECHO_SHARD, Material.NETHERITE_SCRAP, Material.GOLD_INGOT, Material.IRON_INGOT,
        // 方块类
        Material.ENCHANTED_BOOK, Material.NETHER_STAR, Material.TOTEM_OF_UNDYING, 
        Material.TRIDENT, Material.ELYTRA, Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL,
        // 装饰类
        Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL, Material.DRAGON_EGG,
        Material.DRAGON_HEAD, Material.CREEPER_HEAD, Material.PLAYER_HEAD, Material.ZOMBIE_HEAD,
        // 花朵类
        Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM,
        Material.AZURE_BLUET, Material.ORANGE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
        Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.SUNFLOWER,
        Material.ROSE_BUSH, Material.PEONY, Material.LILAC,
        // 珊瑚类
        Material.BRAIN_CORAL, Material.BUBBLE_CORAL, Material.FIRE_CORAL, 
        Material.HORN_CORAL, Material.TUBE_CORAL,
        // 其他装饰品
        Material.HONEYCOMB, Material.HONEY_BOTTLE, Material.EXPERIENCE_BOTTLE,
        Material.ENDER_EYE, Material.ENDER_PEARL, Material.BLAZE_ROD, Material.BLAZE_POWDER,
        Material.MAGMA_CREAM, Material.GHAST_TEAR, Material.FERMENTED_SPIDER_EYE,
        Material.GLOW_BERRIES, Material.SWEET_BERRIES, Material.APPLE, Material.GOLDEN_APPLE,
        Material.ENCHANTED_GOLDEN_APPLE, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE,
        // 音乐唱片
        Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
        Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
        Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
        Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
        Material.MUSIC_DISC_PIGSTEP, Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_5,
        // 彩色物品
        Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE, Material.LIME_DYE,
        Material.GREEN_DYE, Material.CYAN_DYE, Material.LIGHT_BLUE_DYE, Material.BLUE_DYE,
        Material.PURPLE_DYE, Material.MAGENTA_DYE, Material.PINK_DYE, Material.WHITE_DYE
    );

    public LandmarkManager(LandmarkPlugin plugin) {
        this.plugin = plugin;
        this.landmarks = new LinkedHashMap<>();
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
            int index = landmarks.size();
            int row = (index / MENU_COLUMNS) + 1;
            int column = (index % MENU_COLUMNS) + 1;

            // 随机选择一个图标材质
            Material randomIcon = getRandomIconMaterial();

            Landmark landmark = new Landmark(name, location, description, row, column, randomIcon);
            landmarks.put(name.toLowerCase(), landmark);

            normalizeMenuPositions();

            plugin.getSLF4JLogger().info("成功创建锚点展示实体: {} (图标: {})", name, randomIcon.name());
            saveData();
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getSLF4JLogger().error("创建锚点时发生错误: {}", e.getMessage());
            landmarks.remove(name.toLowerCase());
        }
    }
    
    /**
     * 获取一个随机的图标材质
     */
    private Material getRandomIconMaterial() {
        return ICON_MATERIALS.get(random.nextInt(ICON_MATERIALS.size()));
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

            normalizeMenuPositions();

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

                                if (menuRow < 1) {
                                    plugin.getSLF4JLogger().warn("锚点 {} 的菜单行 {} 无效，已调整为 1", key, menuRow);
                                    menuRow = 1;
                                }
                                if (menuColumn < 1 || menuColumn > MENU_COLUMNS) {
                                    plugin.getSLF4JLogger().warn("锚点 {} 的菜单列 {} 超出范围，已调整至有效值", key, menuColumn);
                                    menuColumn = Math.max(1, Math.min(menuColumn, MENU_COLUMNS));
                                }

                                // 加载图标材质，如果不存在则随机生成一个
                                Material iconMaterial;
                                String iconMaterialStr = landmarkSection.getString("icon_material");
                                if (iconMaterialStr != null) {
                                    try {
                                        iconMaterial = Material.valueOf(iconMaterialStr);
                                    } catch (IllegalArgumentException e) {
                                        plugin.getSLF4JLogger().warn("锚点 {} 的图标材质 {} 无效，将随机生成", key, iconMaterialStr);
                                        iconMaterial = getRandomIconMaterial();
                                    }
                                } else {
                                    iconMaterial = getRandomIconMaterial();
                                }

                                Landmark landmark = new Landmark(key, location, description, menuRow, menuColumn, iconMaterial);
                                landmarks.put(key.toLowerCase(), landmark);
                            }
                        } catch (IllegalArgumentException | IllegalStateException e) {
                            plugin.getSLF4JLogger().error("加载锚点 {} 时发生错误: {}", key, e.getMessage());
                        }
                    }
                }

            }
    }

    normalizeMenuPositions();

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
        for (Map.Entry<String, Landmark> entry : getOrderedLandmarks()) {
            ConfigurationSection landmarkSection = landmarksSection.createSection(entry.getKey());
            Landmark landmark = entry.getValue();
            landmarkSection.set("location", landmark.getLocation());
            landmarkSection.set("description", landmark.getDescription());
            landmarkSection.set("menu_row", landmark.getMenuRow());
            landmarkSection.set("menu_column", landmark.getMenuColumn());
            // 保存图标材质
            if (landmark.getIconMaterial() != null) {
                landmarkSection.set("icon_material", landmark.getIconMaterial().name());
            }
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

            normalizeMenuPositions();

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

    public List<Map.Entry<String, Landmark>> getOrderedLandmarks() {
        List<Map.Entry<String, Landmark>> orderedEntries = new ArrayList<>(landmarks.entrySet());
        orderedEntries.sort(Comparator
                .comparingInt((Map.Entry<String, Landmark> entry) -> entry.getValue().getMenuRow())
                .thenComparingInt(entry -> entry.getValue().getMenuColumn())
                .thenComparing(entry -> entry.getValue().getName(), String.CASE_INSENSITIVE_ORDER));
        return orderedEntries;
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
            if (newRow >= 1 && newColumn >= 1 && newColumn <= MENU_COLUMNS) {
                landmark.setMenuRow(newRow);
                landmark.setMenuColumn(newColumn);
                normalizeMenuPositions();
                saveData();
            }
        }
    }

    private void normalizeMenuPositions() {
        if (landmarks.isEmpty()) {
            return;
        }

        List<Map.Entry<String, Landmark>> orderedEntries = new ArrayList<>(landmarks.entrySet());
        orderedEntries.sort(Comparator
                .comparingInt((Map.Entry<String, Landmark> entry) -> entry.getValue().getMenuRow())
                .thenComparingInt(entry -> entry.getValue().getMenuColumn())
                .thenComparing(entry -> entry.getValue().getName(), String.CASE_INSENSITIVE_ORDER));

        Map<String, Landmark> reordered = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, Landmark> entry : orderedEntries) {
            Landmark landmark = entry.getValue();
            int row = (index / MENU_COLUMNS) + 1;
            int column = (index % MENU_COLUMNS) + 1;
            landmark.setMenuRow(row);
            landmark.setMenuColumn(column);
            reordered.put(entry.getKey(), landmark);
            index++;
        }

        landmarks.clear();
        landmarks.putAll(reordered);
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
