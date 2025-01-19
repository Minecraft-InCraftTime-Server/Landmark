package ict.minesunshineone.landmark.command.impl;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class UnlockAllCommand extends SubCommand {

    public UnlockAllCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getConfigManager().sendMessage(sender, "command-player-only", "<red>该命令只能由玩家执行！</red>");
            return;
        }

        Player player = (Player) sender;
        plugin.getLandmarkManager().unlockAllLandmarks(player);
    }

    @Override
    public String getPermission() {
        return "landmark.unlock.all";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
