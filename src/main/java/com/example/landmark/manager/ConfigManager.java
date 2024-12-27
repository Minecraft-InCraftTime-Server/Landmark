package com.example.landmark.manager;

import java.io.File;
import java.io.IOException;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.example.landmark.LandmarkPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;

public class ConfigManager {

    private final LandmarkPlugin plugin;
    private FileConfiguration config;
    private final File configFile;

    private int cooldownTime;
    private int unlockRadius;
    private String dataFileName;
    private Sound unlockSound;
    private float unlockSoundVolume;
    private float unlockSoundPitch;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ConfigManager(LandmarkPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载配置项
        cooldownTime = config.getInt("settings.cooldown-time", 60);
        unlockRadius = config.getInt("settings.unlock-radius", 10);
        dataFileName = config.getString("storage.filename", "landmarks.yml");

        // 声音设置
        String soundName = config.getString("settings.unlock-sound", "ENTITY_PLAYER_LEVELUP");
        try {
            unlockSound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            unlockSound = Sound.ENTITY_PLAYER_LEVELUP;
            plugin.getSLF4JLogger().warn("配置文件中的声音 {} 无效，使用默认声音", soundName);
        }
        unlockSoundVolume = (float) config.getDouble("settings.unlock-sound-volume", 1.0);
        unlockSoundPitch = (float) config.getDouble("settings.unlock-sound-pitch", 1.0);

        validateConfig();
    }

    private void validateConfig() {
        // 检查并修正冷却时间
        if (cooldownTime < 0) {
            cooldownTime = 60;
            plugin.getSLF4JLogger().warn("冷却时间不能为负数，已设置为默认值60秒");
        }

        // 检查并修正解锁半径
        if (unlockRadius <= 0) {
            unlockRadius = 10;
            plugin.getSLF4JLogger().warn("解锁半径必须大于0，已设置为默认值10格");
        }

        // 检查并修正音量和音调
        unlockSoundVolume = Math.max(0.0f, Math.min(1.0f, unlockSoundVolume));
        unlockSoundPitch = Math.max(0.5f, Math.min(2.0f, unlockSoundPitch));
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("无法保存配置文件: {}", e.getMessage());
        }
    }

    public int getCooldownTime() {
        return cooldownTime;
    }

    public int getUnlockRadius() {
        return unlockRadius;
    }

    public Component getMessage(String path, String defaultValue, Object... args) {
        String message = config.getString("messages." + path);
        if (message == null || message.isEmpty()) {
            message = defaultValue;
        }

        // 替换前缀
        String prefix = config.getString("messages.prefix", "<gold>[锚点系统]</gold> ");
        message = message.replace("%prefix%", prefix);

        // 替换其他参数，使用更安全的方式
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length - 1; i += 2) {
                String key = String.valueOf(args[i]);
                String value = String.valueOf(args[i + 1]);
                message = message.replace(key, value);
            }
        }

        try {
            return miniMessage.deserialize(message);
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("消息格式化错误: {}", message);
            return Component.text(message);
        }
    }

    public void sendMessage(CommandSender sender, String path, String defaultValue, Object... args) {
        Component message = getMessage(path, defaultValue, args);
        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else if (message != null) {
            sender.sendMessage(PlainTextComponentSerializer.plainText().serialize(message));
        }
    }

    public Component getPrefix() {
        return getMessage("prefix", "<gold>[锚点系统]</gold> ");
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public Sound getUnlockSound() {
        return unlockSound;
    }

    public float getUnlockSoundVolume() {
        return unlockSoundVolume;
    }

    public float getUnlockSoundPitch() {
        return unlockSoundPitch;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reloadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath) {
        Component title = getMessage(titlePath, "");
        Component subtitle = getMessage(subtitlePath, "");
        player.showTitle(Title.title(title, subtitle));
    }
}
