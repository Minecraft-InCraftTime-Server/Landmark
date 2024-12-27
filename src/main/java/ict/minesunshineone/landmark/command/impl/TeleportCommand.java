package ict.minesunshineone.landmark.command.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class TeleportCommand extends SubCommand {

    public TeleportCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getConfigManager().sendMessage(sender, "command-player-only", "<red>该命令只能由玩家执行！</red>");
            return;
        }

        if (args.length < 1) {
            plugin.getConfigManager().sendMessage(sender, "command-usage", "<red>用法: <usage></red>",
                    "<usage>", "/landmark teleport <锚点名>");
            return;
        }

        Player player = (Player) sender;
        String landmarkName = args[0];
        plugin.getLandmarkManager().teleport(player, landmarkName);
    }

    @Override
    public String getPermission() {
        return "landmark.teleport";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            return plugin.getLandmarkManager().getUnlockedLandmarks((Player) sender).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
