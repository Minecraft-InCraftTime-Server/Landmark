package com.example.landmark.command.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.command.SubCommand;

public class RenameCommand extends SubCommand {

    public RenameCommand(LandmarkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getConfigManager().sendMessage(sender, "command-usage",
                    "<red>用法: <usage></red>", "<usage>", "/landmark rename <旧名称> <新名称>");
            return;
        }

        String oldName = args[0];
        String newName = args[1];

        if (!plugin.getLandmarkManager().getLandmarks().containsKey(oldName.toLowerCase())) {
            plugin.getConfigManager().sendMessage(sender, "landmark-not-exist",
                    "<red>该锚点不存在！</red>");
            return;
        }

        plugin.getLandmarkManager().renameLandmark(oldName, newName);
        plugin.getConfigManager().sendMessage(sender, "rename-success",
                "<green>成功将锚点 <gold><old_name></gold> 重命名为 <gold><new_name></gold>！</green>",
                "<old_name>", oldName,
                "<new_name>", newName);
    }

    @Override
    public String getPermission() {
        return "landmark.rename";
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
