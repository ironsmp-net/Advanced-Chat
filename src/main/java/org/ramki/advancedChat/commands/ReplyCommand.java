/*
package org.ramki.advancedChat.commands;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ramki.advancedChat.AdvancedChat;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    private AdvancedChat main;

    public ReplyCommand(AdvancedChat main){
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        if (sender instanceof Player){
            Player player = (Player) sender;
            if (args.length >= 1){
                if (main.getRecentMessages().containsKey(player.getUniqueId())){
                    UUID uuid = main.getRecentMessages().get(player.getUniqueId());
                    if (Bukkit.getPlayer(uuid) != null){
                        Player target = Bukkit.getPlayer(uuid);
                        StringBuilder message = new StringBuilder();
                        for (int i = 0; i < args.length; i++){
                            message.append(args[i]).append(" ");
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You > " + target.getName() + "&f" + message));
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e" + player.getName() + ": &f" + message));
                    } else {
                        player.sendMessage(ChatColor.RED + "This person is not online!");
                    }
                } else{
                    player.sendMessage(ChatColor.RED + "You have not recived a message!");
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        if (args.length == 1){
            return Collections.singletonList("<message>");
        }

        return List.of();
    }
}
 */