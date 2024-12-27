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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class LandmarkMenu {

    private final LandmarkPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public LandmarkMenu(LandmarkPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        Component title = plugin.getConfigManager().getMessage("gui.title", "<gold>锚点传送菜单</gold>");
        int size = plugin.getConfigManager().getConfig().getInt("gui.size", 54);
        this.inventory = Bukkit.createInventory(null, size, title);
        initializeItems();
    }

    private void initializeItems() {
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

        // 创建当前位置物品
        ItemStack currentLocationItem;
        if (isAtAnyLandmark && currentLandmark != null) {
            currentLocationItem = new ItemStack(Material.ENDER_EYE);
            ItemMeta meta = currentLocationItem.getItemMeta();
            meta.displayName(Component.text("当前锚点: " + currentLandmark.getName()));

            List<Component> lore = new ArrayList<>();
            addUnlockedLore(lore, currentLandmark, currentLandmark.getLocation());
            meta.lore(lore);

            currentLocationItem.setItemMeta(meta);
        } else {
            currentLocationItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = currentLocationItem.getItemMeta();
            meta.displayName(plugin.getConfigManager().getMessage("gui.current-location.not-at-landmark",
                    "<red>未在任何锚点范围内</red>"));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage("gui.lore.not-at-landmark",
                    "<gray>你需要站在已解锁的锚点范围内</gray>"));
            lore.add(Component.empty());
            lore.add(plugin.getConfigManager().getMessage("gui.lore.not-at-landmark-tip",
                    "<gray>才能使用传送魔法</gray>"));
            meta.lore(lore);

            currentLocationItem.setItemMeta(meta);
        }
        inventory.setItem(9, currentLocationItem); // 修改为第一列第二行

        // 设置玻璃隔断（第二列）
        ItemStack separator = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.displayName(Component.empty());
        separator.setItemMeta(separatorMeta);
        for (int i = 0; i < inventory.getSize(); i += 9) {
            inventory.setItem(i + 1, separator);
        }

        // 从第三列开始放置锚点物品
        int slot = 2; // 从第三列开始
        for (Landmark landmark : plugin.getLandmarkManager().getLandmarks().values()) {
            boolean isUnlocked = plugin.getLandmarkManager().isLandmarkUnlocked(player, landmark.getName());
            ItemStack item = createLandmarkItem(landmark, isUnlocked);

            // 跳过第一列和第二列
            while (slot % 9 <= 1) {
                slot++;
            }

            inventory.setItem(slot++, item);
        }
    }

    private ItemStack createLandmarkItem(Landmark landmark, boolean isUnlocked) {
        Material material = Material.valueOf(plugin.getConfigManager().getConfig()
                .getString(isUnlocked ? "gui.items.unlocked.material" : "gui.items.locked.material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // ��接使用锚点名称，不添加格式
        meta.displayName(Component.text(landmark.getName()));

        List<Component> lore = new ArrayList<>();
        Location loc = landmark.getLocation();

        if (isUnlocked) {
            addUnlockedLore(lore, landmark, loc);
        } else {
            addLockedLore(lore, landmark);
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addUnlockedLore(List<Component> lore, Landmark landmark, Location loc) {
        lore.add(plugin.getConfigManager().getMessage("gui.lore.coordinates", "<gray>坐标: X:%x% Y:%y% Z:%z%</gray>")
                .replaceText(builder -> builder.match("%x%").replacement(String.valueOf(loc.getBlockX())))
                .replaceText(builder -> builder.match("%y%").replacement(String.valueOf(loc.getBlockY())))
                .replaceText(builder -> builder.match("%z%").replacement(String.valueOf(loc.getBlockZ()))));

        lore.add(plugin.getConfigManager().getMessage("gui.lore.description", "<gray>描述: %description%</gray>")
                .replaceText(builder -> builder.match("%description%").replacement(landmark.getDescription())));

        lore.add(Component.empty());
        lore.add(plugin.getConfigManager().getMessage("gui.lore.click-teleport", "<yellow>点击���送到此锚点</yellow>"));
    }

    private void addLockedLore(List<Component> lore, Landmark landmark) {
        Location loc = landmark.getLocation();
        lore.add(plugin.getConfigManager().getMessage("gui.lore.coordinates", "<gray>坐标: X:%x% Y:%y% Z:%z%</gray>")
                .replaceText(builder -> builder.match("%x%").replacement(String.valueOf(loc.getBlockX())))
                .replaceText(builder -> builder.match("%y%").replacement(String.valueOf(loc.getBlockY())))
                .replaceText(builder -> builder.match("%z%").replacement(String.valueOf(loc.getBlockZ()))));
        lore.add(Component.empty());
        lore.add(plugin.getConfigManager().getMessage("gui.lore.locked", "<red>尚未解锁</red>"));
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

        if (clickedItem.getType().toString().equals(
                LandmarkPlugin.getInstance().getConfigManager().getConfig().getString(
                        "gui.items.unlocked.material", "ENDER_EYE"))) {
            try {
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) {
                    return;
                }

                // 直接获取纯文本名
                String landmarkName = PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());

                LandmarkPlugin.getInstance().getServer().getGlobalRegionScheduler().execute(
                        LandmarkPlugin.getInstance(), () -> {
                    player.closeInventory();
                    LandmarkPlugin.getInstance().getLandmarkManager().teleport(player, landmarkName);
                });
            } catch (Exception e) {
                LandmarkPlugin.getInstance().getSLF4JLogger().error("GUI传送处理出错: ", e);
            }
        }
    }
}
