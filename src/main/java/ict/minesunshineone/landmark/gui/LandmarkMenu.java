package ict.minesunshineone.landmark.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.model.Landmark;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class LandmarkMenu {

    private final LandmarkPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LandmarkMenu(LandmarkPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Component title = plugin.getConfigManager().getMessage("gui.title", "<gold>锚点传送菜单</gold>");
        int size = plugin.getConfigManager().getConfig().getInt("gui.size", 54);
        this.inventory = Bukkit.createInventory(null, size, title);
        initializeItems();
    }

    private void initializeItems() {
        // 设置边框
        setupBorder();

        // 检查玩家是否在任意锚点范围内
        boolean isAtAnyLandmark = false;
        Landmark currentLandmark = null;

        for (Map.Entry<String, Landmark> entry : plugin.getLandmarkManager().getLandmarks().entrySet()) {
            Landmark landmark = entry.getValue();
            if (plugin.getLandmarkManager().isLandmarkUnlocked(player, entry.getKey())
                    && plugin.getLandmarkManager().isPlayerNearLandmark(player, landmark.getLocation())) {
                isAtAnyLandmark = true;
                currentLandmark = landmark;
                break;
            }
        }

        // 在最后一行中间放置当前位置物品 (第5行中间，索引为40+4=44)
        ItemStack currentLocationItem = createCurrentLocationItem(isAtAnyLandmark, currentLandmark);
        inventory.setItem(40, currentLocationItem);

        // 放置锚点物品（从第2行开始到第4行，跳过边框）
        int slot = 10; // 从第二行第二格开始
        for (Landmark landmark : plugin.getLandmarkManager().getLandmarks().values()) {
            if (slot == 40) {
                break; // 到达最后一行就停止
            }
            // 跳过边框位置
            if (slot % 9 == 0) {
                slot += 2; // 跳过每行的第一格和第二格
            }

            boolean isUnlocked = plugin.getLandmarkManager().isLandmarkUnlocked(player, landmark.getName());
            ItemStack item = createLandmarkItem(landmark, isUnlocked);
            inventory.setItem(slot++, item);
        }
    }

    private void setupBorder() {
        // 获取边框物品配置
        Material borderMaterial = Material.valueOf(
                plugin.getConfigManager().getConfig().getString("gui.border.material", "PURPLE_STAINED_GLASS_PANE"));
        String borderName = plugin.getConfigManager().getConfig().getString("gui.border.name", " ");

        ItemStack border = new ItemStack(borderMaterial);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(miniMessage.deserialize(borderName));
            border.setItemMeta(borderMeta);
        }

        // 设置顶部和底部边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border.clone()); // 顶部
            inventory.setItem(36 + i, border.clone()); // 底部
        }

        // 设置左右边框
        for (int i = 0; i < 4; i++) {
            inventory.setItem(9 * i, border.clone()); // 左边
            inventory.setItem(9 * i + 8, border.clone()); // 右边
        }
    }

    private ItemStack createCurrentLocationItem(boolean isAtAnyLandmark, Landmark currentLandmark) {
        Material material = isAtAnyLandmark
                ? Material.valueOf(plugin.getConfigManager().getConfig().getString("gui.items.current.material", "BEACON"))
                : Material.valueOf(plugin.getConfigManager().getConfig().getString("gui.items.current.locked_material", "BARRIER"));

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (isAtAnyLandmark && currentLandmark != null) {
            String nameFormat = plugin.getConfigManager().getConfig().getString("gui.current-location.at-landmark",
                    "<gradient:gold:yellow>当前位置: %landmark_name%</gradient>");
            meta.displayName(miniMessage.deserialize(nameFormat.replace("%landmark_name%", currentLandmark.getName())));

            List<Component> lore = new ArrayList<>();
            addUnlockedLore(lore, currentLandmark, currentLandmark.getLocation());
            meta.lore(lore);
        } else {
            String nameFormat = plugin.getConfigManager().getConfig().getString("gui.current-location.not-at-landmark",
                    "<red>未在任何锚点范围内</red>");
            meta.displayName(miniMessage.deserialize(nameFormat));

            List<Component> lore = new ArrayList<>();
            lore.add(miniMessage.deserialize(plugin.getConfigManager().getConfig()
                    .getString("gui.lore.not-at-landmark", "<gray>需要站在已解锁的锚点范围内</gray>")));
            meta.lore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLandmarkItem(Landmark landmark, boolean isUnlocked) {
        // 从配置获取物品类型
        String materialPath = isUnlocked ? "gui.items.unlocked.material" : "gui.items.locked.material";
        Material material = Material.valueOf(plugin.getConfigManager().getConfig().getString(materialPath, "DIAMOND"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // 使用配置中的名称格式
        String namePath = isUnlocked ? "gui.items.unlocked.name" : "gui.items.locked.name";
        String nameFormat = plugin.getConfigManager().getConfig().getString(namePath, "");
        if (nameFormat != null) {
            nameFormat = nameFormat.replace("%landmark_name%", landmark.getName());
            meta.displayName(miniMessage.deserialize(nameFormat));
        }

        List<Component> lore = new ArrayList<>();
        Location loc = landmark.getLocation();

        // 添加分隔线
        lore.add(miniMessage.deserialize("<gradient:gold:yellow>━━━━━━━━━━━━━━</gradient>"));

        if (isUnlocked) {
            addUnlockedLore(lore, landmark, loc);
        } else {
            addLockedLore(lore);
        }

        // 添加底部分隔线
        lore.add(miniMessage.deserialize("<gradient:gold:yellow>━━━━━━━━━━━━━━</gradient>"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addUnlockedLore(List<Component> lore, Landmark landmark, Location loc) {
        // 添加世界信息
        String worldFormat = plugin.getConfigManager().getConfig().getString("gui.lore.world", "<aqua>世界: %world%</aqua>");
        if (worldFormat != null && loc.getWorld() != null) {
            lore.add(miniMessage.deserialize(worldFormat.replace("%world%", loc.getWorld().getName())));
        }

        // 使用配置中的坐标格式
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

        // 使用配置中的描述格式
        String descFormat = plugin.getConfigManager().getConfig().getString("gui.lore.description",
                "<gradient:green:aqua>描述: <white>%description%</gradient>");
        if (descFormat != null) {
            String formatted = descFormat.replace("%description%", landmark.getDescription());
            lore.add(miniMessage.deserialize(formatted));
        }

        lore.add(Component.empty());

        // 使用配置中的点击传送提示
        String teleportFormat = plugin.getConfigManager().getConfig().getString("gui.lore.click-teleport",
                "<gradient:gold:yellow>✧ 点击传送 ✧</gradient>");
        if (teleportFormat != null) {
            lore.add(miniMessage.deserialize(teleportFormat));
        }
    }

    private void addLockedLore(List<Component> lore) {
        // 添加底部分隔线
        lore.add(miniMessage.deserialize("<gradient:gold:yellow>━━━━━━━━━━━━━━</gradient>"));

        lore.add(Component.empty());
        lore.add(miniMessage.deserialize("<red>尚未解锁</red>"));
    }

    private void updateMenu() {
        inventory.clear();
        initializeItems();
    }

    public void open() {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            updateMenu();
            player.openInventory(inventory);
        });
    }

    public static void handleClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(LandmarkPlugin.getInstance().getConfigManager()
                .getMessage("gui.title", "<gold>锚点传送菜单</gold>"))) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 检查是否点击的是已解锁的锚点物品
        if (clickedItem.getType().toString().equals(
                LandmarkPlugin.getInstance().getConfigManager().getConfig()
                        .getString("gui.items.unlocked.material", "NETHER_STAR"))) {
            try {
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) {
                    return;
                }

                // 获取纯文本名称并提取锚点名
                String displayName = PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
                // 移除所有格式标记和装饰字符
                String landmarkName = displayName.replaceAll("^[✧\\s]+", "") // 移除开头的✧和空格
                        .replaceAll("[✧\\s]+$", "");  // 移除结尾的✧和空格

                if (!landmarkName.isEmpty()) {
                    LandmarkPlugin.getInstance().getServer().getGlobalRegionScheduler().execute(
                            LandmarkPlugin.getInstance(), () -> {
                        player.closeInventory();
                        LandmarkPlugin.getInstance().getLandmarkManager().teleport(player, landmarkName);
                    });
                }
            } catch (Exception e) {
                LandmarkPlugin.getInstance().getSLF4JLogger().error("GUI传送处理出错: ", e);
            }
        }
    }
}
