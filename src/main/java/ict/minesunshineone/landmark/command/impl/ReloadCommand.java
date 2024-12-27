package ict.minesunshineone.landmark.command.impl;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class ReloadCommand extends SubCommand {

    public ReloadCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getConfigManager().sendMessage(sender, "reload-success",
                    "<green>配置文件重载成功！</green>");
        } catch (Exception e) {
            plugin.getConfigManager().sendMessage(sender, "reload-failed",
                    "<red>配置文件重载失败！请检查控制台错误信息。</red>");
            plugin.getSLF4JLogger().error("重载配置文件时发生错误: ", e);
        }
    }

    @Override
    public String getPermission() {
        return "landmark.reload";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
