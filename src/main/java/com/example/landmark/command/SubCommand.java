package com.example.landmark.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.example.landmark.LandmarkPlugin;

import net.kyori.adventure.text.Component;

public abstract class SubCommand {

    protected final LandmarkPlugin plugin;

    public SubCommand(LandmarkPlugin plugin) {
        this.plugin = plugin;
    }

    protected Component getPrefix() {
        return plugin.getConfigManager().getPrefix();
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + message);
    }

    public abstract void execute(CommandSender sender, String[] args);

    public abstract String getPermission();

    public abstract List<String> onTabComplete(CommandSender sender, String[] args);
}
