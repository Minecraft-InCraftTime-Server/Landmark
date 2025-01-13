package ict.minesunshineone.landmark.command.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class EditCommand extends SubCommand {

    public EditCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getConfigManager().sendMessage(sender, "command-usage", "<red>用法: <usage></red>",
                    "<usage>", "/landmark edit <锚点名> <第一行描述> [第二行描述] [第三行描述]...");
            return;
        }

        String landmarkName = args[0];
        String newDescription = String.join("\n", Arrays.copyOfRange(args, 1, args.length));

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (!plugin.getLandmarkManager().getLandmarks().containsKey(landmarkName.toLowerCase())) {
                plugin.getConfigManager().sendMessage(sender, "landmark-not-exist", "<red>该锚点不存在！</red>");
                return;
            }

            plugin.getLandmarkManager().editLandmarkDescription(landmarkName, newDescription);
            plugin.getConfigManager().sendMessage(sender, "edit-success", "<green>成功编辑锚点 <gold><name></gold> 的描述！</green>",
                    "<name>", landmarkName);
        });
    }

    @Override
    public String getPermission() {
        return "landmark.edit";
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
