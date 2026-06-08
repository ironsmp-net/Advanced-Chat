/*
package org.ramki.advancedChat.commands;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ramki.advancedChat.AdvancedChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageCommand implements TabExecutor {

    private AdvancedChat main;

    public MessageCommand(AdvancedChat main){
        this.main = main;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (sender instanceof Player){
            Player player = (Player) sender;
            if (args.length >= 2){
                if (Bukkit.getPlayerExact(args[0]) != null){
                    Player target = Bukkit.getPlayerExact(args[0]);
                    if (!player.getUniqueId().equals(target.getUniqueId())){
                        StringBuilder message = new StringBuilder();
                        for (int i = 1; i < args.length; i++){
                            message.append(args[i]).append(" ");
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You -> " + target.getName() + "&f" + message));
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e" + player.getName() + ": &f" + message));
                        main.getRecentMessages().put(target.getUniqueId(), player.getUniqueId());
                    } else{
                        player.sendMessage(ChatColor.RED + "You cannot message yourself!");
                    }
                } else{
                    player.sendMessage(ChatColor.RED + "This player is not online!");
                }
            } else{
                player.sendMessage(ChatColor.RED + "Invalid Command, use /message <player> <message>");
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        if (args.length == 1){
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()){
                players.add(p.getName());
            }
            return players;
        } else if (args.length == 2){
            return Collections.singletonList("<message>");
        }

        return List.of();
    }
}
 */