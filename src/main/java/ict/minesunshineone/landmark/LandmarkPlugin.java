package ict.minesunshineone.landmark;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import ict.minesunshineone.landmark.command.LandmarkCommand;
import ict.minesunshineone.landmark.listener.PlayerListener;
import ict.minesunshineone.landmark.manager.ConfigManager;
import ict.minesunshineone.landmark.manager.LandmarkManager;

public class LandmarkPlugin extends JavaPlugin {

    private static LandmarkPlugin instance;
    private LandmarkManager landmarkManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // 确保插件数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 初始化配置管理器
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        // 初始化锚点管理器
        this.landmarkManager = new LandmarkManager(this);
        this.landmarkManager.loadData();

        // 注册命令
        registerCommands();

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 添加定期清理任务
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            landmarkManager.cleanupInactivePlayers(24 * 60 * 60 * 1000); // 24小时
            landmarkManager.cleanupCooldowns();
        }, 20 * 60 * 60, 20 * 60 * 60); // 每小时执行一次

        getLogger().info("锚点传送插件已启用！");
    }

    @Override
    public void onDisable() {
        // 保存数据
        if (landmarkManager != null) {
            landmarkManager.saveData();
            landmarkManager.cleanupCooldowns();
        }
        getLogger().info("锚点传送插件已禁用！");
    }

    @Override
    public void onLoad() {
        // 确保插件数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    }

    private void registerCommands() {
        PluginCommand command = getCommand("landmark");
        if (command != null) {
            command.setExecutor(new LandmarkCommand(this));
        }
    }

    public static LandmarkPlugin getInstance() {
        return instance;
    }

    public LandmarkManager getLandmarkManager() {
        return landmarkManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
