package ict.minesunshineone.landmark.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private static final String NAVIGATION_KEY = "landmark_nav";
    private static final String NAV_PREVIOUS = "previous";
    private static final String NAV_NEXT = "next";
    private static final int INVENTORY_COLUMNS = 9;
    private static final int MENU_COLUMNS = 7;
    private final LandmarkPlugin plugin;
    private final UUID playerId;
    private Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey landmarkKey;
    private final NamespacedKey navigationKey;
    private int currentPage = 0;
    private int rowsPerPage;
    private int slotsPerPage;
    private int totalPages = 1;

    public LandmarkMenu(LandmarkPlugin plugin, Player player) {
        this.plugin = plugin;
        this.playerId = player.getUniqueId();
        this.landmarkKey = new NamespacedKey(plugin, LANDMARK_KEY);
        this.navigationKey = new NamespacedKey(plugin, NAVIGATION_KEY);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        Component title = plugin.getConfigManager().getMessage("gui.title", "<gold>锚点传送菜单</gold>");
        int size = plugin.getConfigManager().getConfig().getInt("gui.size", 54);
        this.inventory = Bukkit.createInventory(this, size, title);
        this.currentPage = 0;
        initializeItems();
        getPlayer().openInventory(inventory);
    }

    private void initializeItems() {
        if (inventory == null) {
            return;
        }

        inventory.clear();
        setupBorder();

        List<Map.Entry<String, Landmark>> orderedLandmarks = plugin.getLandmarkManager().getOrderedLandmarks();
        recomputePagination(orderedLandmarks.size());
        setupLandmarks(orderedLandmarks);
        setupNavigationControls();
        setupCurrentLocation();
    }

    private void setupBorder() {
        ItemStack border = createBorderItem();

        int rows = Math.max(1, inventory.getSize() / INVENTORY_COLUMNS);
        int lastRowStart = (rows - 1) * INVENTORY_COLUMNS;

        // 设置顶部和底部边框
        for (int column = 0; column < INVENTORY_COLUMNS; column++) {
            inventory.setItem(column, border.clone());
            inventory.setItem(lastRowStart + column, border.clone());
        }

        // 设置左右边框
        for (int row = 1; row < rows - 1; row++) {
            int rowStart = row * INVENTORY_COLUMNS;
            inventory.setItem(rowStart, border.clone());
            inventory.setItem(rowStart + (INVENTORY_COLUMNS - 1), border.clone());
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
        Player player = getPlayer();
        if (player == null) {
            return;
        }

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
        int centerColumn = INVENTORY_COLUMNS / 2;
        int centerSlot = (inventory.getSize() - INVENTORY_COLUMNS) + centerColumn;
        if (centerSlot >= 0 && centerSlot < inventory.getSize()) {
            inventory.setItem(centerSlot, currentLocationItem);
        }
    }

    private void setupLandmarks(List<Map.Entry<String, Landmark>> orderedLandmarks) {
        if (slotsPerPage <= 0) {
            return;
        }

        Player player = getPlayer();
        if (player == null) {
            return;
        }

        int startIndex = currentPage * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, orderedLandmarks.size());

        for (int index = startIndex; index < endIndex; index++) {
            Map.Entry<String, Landmark> entry = orderedLandmarks.get(index);
            Landmark landmark = entry.getValue();

            int offset = index - startIndex;
            int row = (offset / MENU_COLUMNS) + 1;
            int col = (offset % MENU_COLUMNS) + 1;
            int slot = (row * INVENTORY_COLUMNS) + col;
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            boolean isUnlocked = plugin.getLandmarkManager().isLandmarkUnlocked(player, entry.getKey());
            ItemStack item = createLandmarkItem(landmark, isUnlocked);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(landmarkKey, PersistentDataType.STRING, entry.getKey());
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }
    }

    private void recomputePagination(int totalLandmarks) {
        rowsPerPage = Math.max(1, (inventory.getSize() / INVENTORY_COLUMNS) - 2);
        slotsPerPage = Math.max(1, rowsPerPage * MENU_COLUMNS);
        totalPages = Math.max(1, (int) Math.ceil(totalLandmarks / (double) slotsPerPage));
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
    }

    private void setupNavigationControls() {
        int bottomRowStart = inventory.getSize() - INVENTORY_COLUMNS;
        if (bottomRowStart < 0) {
            return;
        }

        ItemStack indicator = createPageIndicatorItem();
        inventory.setItem(bottomRowStart + (INVENTORY_COLUMNS - 1), indicator);

        if (totalPages <= 1) {
            return;
        }

        ItemStack previous = createNavigationItem(
                "<#c7a3ed><bold>上一页</bold>",
                "<gray><bold>已是第一页</bold>",
                NAV_PREVIOUS,
                currentPage > 0);
        ItemStack next = createNavigationItem(
                "<#c7a3ed><bold>下一页</bold>",
                "<gray><bold>已是最后一页</bold>",
                NAV_NEXT,
                currentPage < totalPages - 1);

        inventory.setItem(bottomRowStart + 1, previous);
        inventory.setItem(bottomRowStart + (INVENTORY_COLUMNS - 2), next);
    }

    private ItemStack createNavigationItem(String activeName, String inactiveName, String action, boolean enabled) {
        Material material = enabled ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE;
        String displayName = enabled ? activeName : inactiveName;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(displayName));
            if (enabled) {
                meta.getPersistentDataContainer().set(navigationKey, PersistentDataType.STRING, action);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPageIndicatorItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = String.format("<#c7a3ed><bold>第 %d / %d 页</bold>", currentPage + 1, totalPages);
            meta.displayName(miniMessage.deserialize(title));
            item.setItemMeta(meta);
        }
        return item;
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
        String navigationAction = container.get(navigationKey, PersistentDataType.STRING);
        if (navigationAction != null) {
            handleNavigationClick(navigationAction);
            return;
        }

        String landmarkName = container.get(landmarkKey, PersistentDataType.STRING);

        if (landmarkName != null && plugin.getLandmarkManager().isLandmarkUnlocked(player, landmarkName)) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                player.closeInventory();
                plugin.getLandmarkManager().teleport(player, landmarkName);
            });
        }
    }

    private void handleNavigationClick(String action) {
        if (NAV_PREVIOUS.equals(action) && currentPage > 0) {
            currentPage--;
            initializeItems();
        } else if (NAV_NEXT.equals(action) && currentPage < totalPages - 1) {
            currentPage++;
            initializeItems();
        }
    }

    private Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

}
