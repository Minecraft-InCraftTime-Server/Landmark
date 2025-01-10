package ict.minesunshineone.landmark.command.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class DeleteCommand extends SubCommand {

    public DeleteCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getConfigManager().sendMessage(sender, "command-usage",
                    "<red>用法: <usage></red>", "<usage>", "/landmark delete <名称>");
            return;
        }

        String name = args[0];
        if (!plugin.getLandmarkManager().getLandmarks().containsKey(name.toLowerCase())) {
            plugin.getConfigManager().sendMessage(sender, "landmark-not-exist",
                    "<red>该锚点不存在！</red>");
            return;
        }

        // 获取锚点位置
        Location location = plugin.getLandmarkManager().getLandmarks().get(name.toLowerCase()).getLocation();
        if (location != null && location.getWorld() != null) {
            // 在锚点所在区域执行删除操作
            plugin.getServer().getRegionScheduler().execute(plugin, location, () -> {
                plugin.getLandmarkManager().deleteLandmark(name);
                plugin.getConfigManager().sendMessage(sender, "delete-success",
                        "<green>成功删除锚点 <gold><name></gold>！</green>",
                        "<name>", name);
            });
        }
    }

    @Override
    public String getPermission() {
        return "landmark.delete";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getLandmarkManager().getLandmarks().keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
