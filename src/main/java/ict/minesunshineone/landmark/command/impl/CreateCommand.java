package ict.minesunshineone.landmark.command.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.SubCommand;

public class CreateCommand extends SubCommand {

    public CreateCommand(LandmarkPlugin plugin) {
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
                    "<usage>", "/landmark create <名称> [描述]");
            return;
        }

        Player player = (Player) sender;
        String name = args[0];
        String description = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "暂无描述";

        plugin.getServer().getRegionScheduler().execute(plugin, player.getLocation(), () -> {
            if (plugin.getLandmarkManager().getLandmarks().containsKey(name.toLowerCase())) {
                plugin.getConfigManager().sendMessage(sender, "landmark-exists",
                        "<red>该锚点名称已存在！</red>");
                return;
            }

            plugin.getLandmarkManager().createLandmark(name, player.getLocation(), description);
            plugin.getConfigManager().sendMessage(sender, "create-success",
                    "<green>成功创建锚点 <gold><name></gold>！</green>",
                    "<name>", name);
        });
    }

    @Override
    public String getPermission() {
        return "landmark.create";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
