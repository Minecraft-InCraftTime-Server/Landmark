package ict.minesunshineone.landmark.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.command.impl.CreateCommand;
import ict.minesunshineone.landmark.command.impl.DeleteCommand;
import ict.minesunshineone.landmark.command.impl.EditCommand;
import ict.minesunshineone.landmark.command.impl.MenuCommand;
import ict.minesunshineone.landmark.command.impl.ReloadCommand;
import ict.minesunshineone.landmark.command.impl.RenameCommand;
import ict.minesunshineone.landmark.command.impl.TeleportCommand;
import ict.minesunshineone.landmark.command.impl.UnlockAllCommand;

public class LandmarkCommand implements CommandExecutor, TabCompleter {

    private final LandmarkPlugin plugin;
    private final Map<String, SubCommand> subCommands;

    public LandmarkCommand(LandmarkPlugin plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("create", new CreateCommand(plugin));
        subCommands.put("teleport", new TeleportCommand(plugin));
        subCommands.put("menu", new MenuCommand(plugin));
        subCommands.put("delete", new DeleteCommand(plugin));
        subCommands.put("rename", new RenameCommand(plugin));
        subCommands.put("edit", new EditCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("unlockall", new UnlockAllCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage("§c未知的子命令！使用 /landmark help 查看帮助。");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(cmd -> sender.hasPermission(subCommands.get(cmd).getPermission()))
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
            return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        return Collections.emptyList();
    }

    private void sendHelpMessage(CommandSender sender) {
        showHelp(sender);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("help.separator", "<gold>================================</gold>"));

        // 基础命令（所有玩家都能看到）
        String[] basicCommands = {
            "/landmark teleport <名称>",
            "/landmark menu"
        };

        // 管理命令（只有管理员能看到）
        String[] adminCommands = {
            "/landmark create <名称> [描述]",
            "/landmark delete <名称>",
            "/landmark rename <旧名称> <新名称>",
            "/landmark edit <名称> <描述>",
            "/landmark reload"
        };

        // 显示基础命令
        for (String cmd : basicCommands) {
            sender.sendMessage(plugin.getConfigManager().getMessage("help.command",
                    "<gold>[锚点系统]</gold> <yellow>%command%</yellow>",
                    "%command%", cmd));
        }

        // 如果有管理员权限，显示管理命令
        if (sender.hasPermission("landmark.admin")) {
            for (String cmd : adminCommands) {
                sender.sendMessage(plugin.getConfigManager().getMessage("help.command",
                        "<gold>[锚点系统]</gold> <yellow>%command%</yellow>",
                        "%command%", cmd));
            }
        }
    }
}
