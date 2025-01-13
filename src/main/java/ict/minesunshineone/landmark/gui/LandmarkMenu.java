package ict.minesunshineone.landmark.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.model.Landmark;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LandmarkMenu implements InventoryHolder {

    private static final String LANDMARK_KEY = "landmark_name";
    private final LandmarkPlugin plugin;
    private final Player player;
    private Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey landmarkKey;

    public LandmarkMenu(LandmarkPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.landmarkKey = new NamespacedKey(plugin, LANDMARK_KEY);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        Component title = plugin.getConfigManager().getMessage("gui.title", "<gold>锚点传送菜单</gold>");
        int size = plugin.getConfigManager().getConfig().getInt("gui.size", 54);
        this.inventory = Bukkit.createInventory(this, size, title);
        initializeItems();
        player.openInventory(inventory);
    }

    private void initializeItems() {
        setupBorder();
        setupCurrentLocation();
        setupLandmarks();
    }

    private void setupBorder() {
        ItemStack border = createBorderItem();

        // 设置顶部和底部边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border.clone());
            inventory.setItem(36 + i, border.clone());
        }

        // 设置左右边框
        for (int i = 1; i < 4; i++) {
            inventory.setItem(9 * i, border.clone());
            inventory.setItem(9 * i + 8, border.clone());
        }
    }

    private ItemStack createBorderItem() {
        Material borderMaterial = Material.valueOf(
                plugin.getConfigManager().getConfig().getString("gui.border.material", "PURPLE_STAINED_GLASS_PANE"));
        String borderName = plugin.getConfigManager().getConfig().getString("gui.border.name", " ");

        ItemStack border = new ItemStack(borderMaterial);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null && borderName != null) {
            borderMeta.displayName(miniMessage.deserialize(borderName));
            border.setItemMeta(borderMeta);
        }
        return border;
    }

    private void setupCurrentLocation() {
        Landmark currentLandmark = null;
        boolean isAtAnyLandmark = false;

        for (Map.Entry<String, Landmark> entry : plugin.getLandmarkManager().getLandmarks().entrySet()) {
            if (plugin.getLandmarkManager().isLandmarkUnlocked(player, entry.getKey())
                    && plugin.getLandmarkManager().isPlayerNearLandmark(player, entry.getValue().getLocation())) {
                isAtAnyLandmark = true;
                currentLandmark = entry.getValue();
                break;
            }
        }

        ItemStack currentLocationItem = createCurrentLocationItem(isAtAnyLandmark, currentLandmark);
        inventory.setItem(40, currentLocationItem);
    }

    private void setupLandmarks() {
        for (Map.Entry<String, Landmark> entry : plugin.getLandmarkManager().getLandmarks().entrySet()) {
            Landmark landmark = entry.getValue();
            int row = landmark.getMenuRow();
            int col = landmark.getMenuColumn();

            // 确保位置在有效范围内（第2-4行，即索引1-3，列1-7）
            if (row < 1 || row > 3 || col < 1 || col > 7) {
                plugin.getSLF4JLogger().warn("锚点 {} 的位置 ({}, {}) 超出有效范围，将在下次加载时自动修复",
                        landmark.getName(), row, col);
                continue;
            }

            // 计算实际槽位（row对应第2-4行）
            int slot = (row * 9) + col;
            boolean isUnlocked = plugin.getLandmarkManager().isLandmarkUnlocked(player, entry.getKey());
            ItemStack item = createLandmarkItem(landmark, isUnlocked);

            // 存储锚点名称到物品中
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(landmarkKey, PersistentDataType.STRING, entry.getKey());
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }
    }

    private ItemStack createCurrentLocationItem(boolean isAtAnyLandmark, Landmark currentLandmark) {
        Material material = isAtAnyLandmark
                ? Material.valueOf(Objects.requireNonNull(plugin.getConfigManager().getConfig().getString("gui.items.current.material", "CONDUIT")))
                : Material.valueOf(Objects.requireNonNull(plugin.getConfigManager().getConfig().getString("gui.items.current.locked_material", "BARRIER")));

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<Component> lore;
        if (isAtAnyLandmark && currentLandmark != null) {
            lore = plugin.getConfigManager().getMultilineMessage(
                    "gui.current-location.at-landmark",
                    "",
                    "%landmark_name%", currentLandmark.getName()
            );
        } else {
            lore = plugin.getConfigManager().getMultilineMessage(
                    "gui.current-location.not-at-landmark",
                    ""
            );
        }

        if (!lore.isEmpty()) {
            meta.displayName(lore.get(0));
            if (lore.size() > 1) {
                meta.lore(lore.subList(1, lore.size()));
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLandmarkItem(Landmark landmark, boolean isUnlocked) {
        String materialPath = isUnlocked ? "gui.items.unlocked.material" : "gui.items.locked.material";
        Material material = Material.valueOf(Objects.requireNonNull(plugin.getConfigManager().getConfig().getString(materialPath, "DIAMOND")));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String namePath = isUnlocked ? "gui.items.unlocked.name" : "gui.items.locked.name";
        String nameFormat = plugin.getConfigManager().getConfig().getString(namePath, "");
        if (nameFormat != null) {
            nameFormat = nameFormat.replace("%landmark_name%", landmark.getName());
            meta.displayName(miniMessage.deserialize(nameFormat));
        }

        List<Component> lore = new ArrayList<>();
        Location loc = landmark.getLocation();

        lore.add(miniMessage.deserialize("<#c7a3ed><bold>﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍</bold>"));

        if (isUnlocked) {
            addUnlockedLore(lore, landmark, loc);
        } else {
            addLockedLore(lore, landmark);
        }

        lore.add(miniMessage.deserialize("<#c7a3ed><bold>﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍﹍</bold>"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addUnlockedLore(List<Component> lore, Landmark landmark, Location loc) {
        if (loc.getWorld() != null) {
            String worldFormat = plugin.getConfigManager().getConfig().getString("gui.lore.world", "<aqua>世界: %world%</aqua>");
            if (worldFormat != null) {
                lore.add(miniMessage.deserialize(worldFormat.replace("%world%", loc.getWorld().getName())));
            }
        }

        String coordFormat = plugin.getConfigManager().getConfig().getString("gui.lore.coordinates",
                "<gradient:aqua:blue>坐标: <white>X:%x% Y:%y% Z:%z%</gradient>");
        if (coordFormat != null) {
            String formatted = coordFormat
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));
            lore.add(miniMessage.deserialize(formatted));
        }

        lore.add(Component.empty());

        String descFormat = plugin.getConfigManager().getConfig().getString("gui.lore.description",
                "<gradient:green:aqua>描述: <white>%description%</gradient>");
        if (descFormat != null) {
            String[] descriptionLines = landmark.getDescription().split("\n");
            if (descriptionLines.length == 1) {
                // 单行文本直接显示
                String formatted = descFormat.replace("%description%", descriptionLines[0].trim());
                lore.add(miniMessage.deserialize(formatted));
            } else if (descriptionLines.length > 1) {
                // 多行文本先显示"描述:"，然后每行单独显示
                String formatted = descFormat.replace("%description%", "");  // 使用相同格式，但不显示描述内容
                lore.add(miniMessage.deserialize(formatted));
                for (String line : descriptionLines) {
                    lore.add(miniMessage.deserialize("<white><bold>  " + line.trim() + "</bold></white>"));  // 添加4个空格作为缩进
                }
            }
        }

        lore.add(Component.empty());

        String teleportFormat = plugin.getConfigManager().getConfig().getString("gui.lore.click-teleport",
                "<gradient:gold:yellow>✧ 点击传送 ✧</gradient>");
        if (teleportFormat != null) {
            lore.add(miniMessage.deserialize(teleportFormat));
        }
    }

    private void addLockedLore(List<Component> lore, Landmark landmark) {
        String worldFormat = plugin.getConfigManager().getConfig().getString("gui.lore.world",
                "<#c7a3ed><bold>• 位置</bold> <gray><bold>???</bold>");
        if (worldFormat != null && landmark.getLocation().getWorld() != null) {
            lore.add(miniMessage.deserialize(worldFormat.replace("%world%", "???")));
        }

        Location loc = landmark.getLocation();
        String coordFormat = plugin.getConfigManager().getConfig().getString("gui.lore.coordinates",
                "<#c7a3ed><bold>• 坐标</bold> <white><bold>%x% %y% %z%</bold>");
        if (coordFormat != null) {
            String formatted = coordFormat
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));
            lore.add(miniMessage.deserialize(formatted));
        }

        lore.add(Component.empty());

        String descFormat = plugin.getConfigManager().getConfig().getString("gui.lore.description",
                "<#c7a3ed><bold>• 描述</bold> <gray><bold>???</bold>");
        if (descFormat != null) {
            lore.add(miniMessage.deserialize(descFormat.replace("%description%", "???")));
        }

        lore.add(Component.empty());

        String lockedFormat = plugin.getConfigManager().getConfig().getString("gui.lore.locked",
                "<gray><bold>该锚点尚未解封</bold>");
        if (lockedFormat != null) {
            lore.add(miniMessage.deserialize(lockedFormat));
        }
    }

    public void onClick(int slot, InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        try {
            handleItemClick(clickedItem, (Player) event.getWhoClicked());
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("GUI传送处理出错: {}", e.getMessage(), e);
        }
    }

    private void handleItemClick(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String landmarkName = container.get(landmarkKey, PersistentDataType.STRING);

        if (landmarkName != null && plugin.getLandmarkManager().isLandmarkUnlocked(player, landmarkName)) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                player.closeInventory();
                plugin.getLandmarkManager().teleport(player, landmarkName);
            });
        }
    }

}
