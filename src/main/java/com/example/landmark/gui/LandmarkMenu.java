package com.example.landmark.gui;

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

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.model.Landmark;

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
        Map<String, Landmark> landmarks = plugin.getLandmarkManager().getLandmarks();
        int slot = 0;

        for (Landmark landmark : landmarks.values()) {
            boolean isUnlocked = plugin.getLandmarkManager().isLandmarkUnlocked(player, landmark.getName());
            ItemStack item = createLandmarkItem(landmark, isUnlocked);
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

        // 直接使用锚点名称，不添加格式
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
        lore.add(plugin.getConfigManager().getMessage("gui.lore.click-teleport", "<yellow>点击传送到此锚点</yellow>"));
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

                // 直接获取纯文本名称
                String landmarkName = PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());

                LandmarkPlugin.getInstance().getServer().getGlobalRegionScheduler().execute(
                        LandmarkPlugin.getInstance(), () -> {
                    player.closeInventory();
                    LandmarkPlugin.getInstance().getLandmarkManager().teleport(player, landmarkName);
                });
            } catch (Exception e) {
                LandmarkPlugin.getInstance().getSLF4JLogger().error("GUI传送处理出错: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
