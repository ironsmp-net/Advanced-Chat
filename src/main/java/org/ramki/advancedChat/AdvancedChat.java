package org.ramki.advancedChat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdvancedChat extends JavaPlugin implements TabExecutor {

    private static AdvancedChat instance;
    private boolean papiEnabled;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        papiEnabled = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (papiEnabled) {
            getLogger().info("PlaceholderAPI found, hooking in.");
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getCommand("advchat").setExecutor(this);
        getCommand("advchat").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("advchat.reload")) {
                sender.sendRichMessage("<red>No permission.");
                return true;
            }

            reloadConfig();
            papiEnabled = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
            sender.sendRichMessage("<green>AdvancedChat has been reloaded.");
            return true;
        }

        sender.sendRichMessage("<red>Usage: /advchat reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advchat.reload")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }

    public static AdvancedChat getInstance() {
        return instance;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }
}
