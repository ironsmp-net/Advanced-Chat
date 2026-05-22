package org.ramki.advancedChat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.ramki.advancedChat.config.ChatPluginSettings;
import org.ramki.advancedChat.service.CooldownService;
import org.ramki.advancedChat.storage.ChatDatabase;
import org.ramki.advancedChat.task.ChatRelayTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    }

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
