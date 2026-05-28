package org.ramki.advancedChat.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.storage.ChatDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatRelayTask {

    private final AdvancedChat plugin;
    private final ChatDatabase database;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicLong lastSeenId;

    private BukkitTask pollTask;

    public ChatRelayTask(AdvancedChat plugin, ChatDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.lastSeenId = new AtomicLong(database.currentMaxId());
    }

    public void start() {
        long interval = Math.max(1L, this.plugin.getSettings().database().pollIntervalTicks());
        this.pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            if (!this.plugin.getSettings().chatSync()) {
                long maxId = this.database.currentMaxId();
                if (maxId > this.lastSeenId.get()) this.lastSeenId.set(maxId);
                return;
            }

            List<ChatDatabase.StoredMessage> messages = this.database.pollSince(this.lastSeenId.get());
            if (messages.isEmpty()) return;

            List<Component> rendered = new ArrayList<>(messages.size());
            long highest = this.lastSeenId.get();
            for (ChatDatabase.StoredMessage message : messages) {
                rendered.add(this.miniMessage.deserialize(message.renderedMiniMessage()));
                if (message.id() > highest) highest = message.id();
            }

            long finalHighest = highest;
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (Component component : rendered) {
                    this.plugin.getServer().sendMessage(component);
                }
                this.lastSeenId.set(finalHighest);
            });
        }, interval, interval);
    }

    public void stop() {
        if (this.pollTask != null) {
            this.pollTask.cancel();
            this.pollTask = null;
        }
    }
}