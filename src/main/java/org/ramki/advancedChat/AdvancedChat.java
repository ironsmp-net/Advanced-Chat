package org.ramki.advancedChat;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
//import org.ramki.advancedChat.commands.MessageCommand;
//import org.ramki.advancedChat.commands.ReplyCommand;
import org.ramki.advancedChat.config.ChatPluginSettings;
import org.ramki.advancedChat.service.CooldownService;
import org.ramki.advancedChat.storage.ChatDatabase;
import org.ramki.advancedChat.task.ChatRelayTask;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class AdvancedChat extends JavaPlugin implements TabExecutor {

    private static AdvancedChat instance;
    private volatile ChatPluginSettings settings;
    private boolean papiEnabled;

    private CooldownService cooldownService;
    private ExecutorService executor;
    private ChatDatabase database;
    private ChatRelayTask relayTask;

    private HashMap<UUID, UUID> recentMessages;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        getRecentMessages().remove(e.getPlayer().getUniqueId());
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        this.settings = ChatPluginSettings.load(getConfig());

        this.papiEnabled = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (this.papiEnabled) {
            getLogger().info("PlaceholderAPI found, hooking in.");
        }

        this.cooldownService = new CooldownService(this);
        startDatabase();

        getServer().getPluginManager().registerEvents(
                new ChatListener(this, this.cooldownService, this::getDatabase), this);

        getCommand("advchat").setExecutor(this);
        getCommand("advchat").setTabCompleter(this);
        //getCommand("message").setExecutor(new MessageCommand(this));
        //getCommand("message").setTabCompleter(new MessageCommand(this));
        //getCommand("reply").setExecutor(new ReplyCommand(this));
        //getCommand("reply").setTabCompleter(new ReplyCommand(this));
        recentMessages = new HashMap<>();

        /*
        this is the message, msg, dm command
         */
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(
                Commands.literal("message")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) ->{
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    String message = StringArgumentType.getString(ctx, "message");
                                    CommandSender sender = ctx.getSource().getSender();
                                    if (sender instanceof Player){
                                        Player player = (Player) sender;
                                        Player target = Bukkit.getPlayerExact(playerName);
                                        if (target == null){
                                            player.sendMessage(ChatColor.RED + "This player is not online!");
                                            return 1;
                                        }
                                        if (player.getUniqueId().equals(target.getUniqueId())) {
                                            player.sendMessage(ChatColor.RED + "You cannot message yourself!");
                                            return 1;
                                        }
                                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You -> " + target.getName() + "&f" + message));
                                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e" + player.getName() + ": &f" + message));
                                        this.getRecentMessages().put(target.getUniqueId(), player.getUniqueId());
                                    }
                                    return 1;
                                })
                            )
                        )
                        .build(),
                    List.of("msg", "dm")
            );
        });

        /*
        this is the reply, r command
         */
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->{
            commands.registrar().register(
                    Commands.literal("reply")
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        String message = StringArgumentType.getString(ctx, "message");
                                        CommandSender sender = ctx.getSource().getSender();
                                        if (sender instanceof Player){
                                            Player player = (Player) sender;
                                            if (this.getRecentMessages().containsKey(player.getUniqueId())){
                                                UUID uuid = this.getRecentMessages().get(player.getUniqueId());
                                                if (Bukkit.getPlayer(uuid) != null){
                                                    Player target = Bukkit.getPlayer(uuid);
                                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You > " + target.getName() + "&f" + message));
                                                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e" + player.getName() + ": &f" + message));
                                                    return 1;
                                                } else {
                                                    player.sendMessage(ChatColor.RED + "This player is offline!");
                                                    return 1;
                                                }
                                            }
                                        } else {
                                            Bukkit.getConsoleSender().sendRichMessage("<red>You Cannot use this command in the console");
                                            return 1;
                                        }
                                        return 1;
                                    })
                            )
                            .build(),
                    List.of("r"));
        });
    }

    public HashMap<UUID, UUID> getRecentMessages(){ return recentMessages; }

    @Override
    public void onDisable() {
        if (this.relayTask != null) {
            this.relayTask.stop();
            this.relayTask = null;
        }

        if (this.executor != null) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                    getLogger().warning("Executor did not terminate gracefully, forced shutdown");
                }
            } catch (InterruptedException ex) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
                getLogger().log(Level.WARNING, "Executor shutdown interrupted", ex);
            }
            this.executor = null;
        }

        if (this.database != null) {
            this.database.shutdown();
            this.database = null;
        }

        if (this.cooldownService != null) {
            this.cooldownService.clear();
        }
    }

    private void startDatabase() {
        if (!this.settings.database().enabled()) return;

        try {
            this.executor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "advchat-worker");
                thread.setDaemon(true);
                return thread;
            });
            this.database = new ChatDatabase(this, this.executor, getLogger());
            this.relayTask = new ChatRelayTask(this, this.database);
            this.relayTask.start();
            getLogger().info("Cross-server chat sync enabled (server-name: " + this.settings.serverName() + ")");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize chat database — cross-server sync disabled", ex);
            shutdownDatabaseSilently();
        }
    }

    private void shutdownDatabaseSilently() {
        if (this.relayTask != null) {
            this.relayTask.stop();
            this.relayTask = null;
        }
        if (this.database != null) {
            try { this.database.shutdown(); } catch (Exception ignored) {}
            this.database = null;
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("advchat.reload")) {
                sender.sendRichMessage("<red>No permission.");
                return true;
            }

            reloadConfig();
            this.settings = ChatPluginSettings.load(getConfig());
            this.papiEnabled = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
            sender.sendRichMessage("<green>AdvancedChat reloaded. Database toggle/credentials require a full restart.");
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

    public ChatPluginSettings getSettings() {
        return this.settings;
    }

    public ChatDatabase getDatabase() {
        return this.database;
    }

    public boolean isPapiEnabled() {
        return this.papiEnabled;
    }
}
