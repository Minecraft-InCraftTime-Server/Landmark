package com.example.landmark.command;

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

import com.example.landmark.LandmarkPlugin;
import com.example.landmark.command.impl.CreateCommand;
import com.example.landmark.command.impl.DeleteCommand;
import com.example.landmark.command.impl.EditCommand;
import com.example.landmark.command.impl.MenuCommand;
import com.example.landmark.command.impl.ReloadCommand;
import com.example.landmark.command.impl.RenameCommand;
import com.example.landmark.command.impl.TeleportCommand;

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

        for (String cmd : Arrays.asList(
                "/landmark create <名称> [描述]",
                "/landmark teleport <名称>",
                "/landmark menu",
                "/landmark delete <名称>",
                "/landmark rename <旧名称> <新名称>",
                "/landmark edit <名称> <描述>")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("help.command",
                    "<gold>[锚点系统]</gold> <yellow>%command%</yellow> <gray>- %description%</gray>",
                    "%command%", cmd));
        }
    }
}
