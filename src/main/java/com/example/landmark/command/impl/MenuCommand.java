package com.example.landmark.command.impl;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.command.SubCommand;
import com.example.landmark.gui.LandmarkMenu;

public class MenuCommand extends SubCommand {

    public MenuCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getConfigManager().sendMessage(sender, "command-player-only", "<red>该命令只能由玩家执行！</red>");
            return;
        }

        Player player = (Player) sender;
        new LandmarkMenu(plugin, player).open();
    }

    @Override
    public String getPermission() {
        return "landmark.menu";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
