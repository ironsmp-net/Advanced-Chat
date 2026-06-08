package org.ramki.advancedChat;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ramki.advancedChat.config.ChatPluginSettings;
import org.ramki.advancedChat.service.CooldownService;
import org.ramki.advancedChat.storage.ChatDatabase;
import net.kyori.adventure.audience.Audience;

import java.util.Locale;
import java.util.function.Supplier;

public final class ChatListener implements Listener {

    private final AdvancedChat plugin;
    private final CooldownService cooldownService;
    private final Supplier<ChatDatabase> databaseSupplier;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ChatListener(AdvancedChat plugin, CooldownService cooldownService, Supplier<ChatDatabase> databaseSupplier) {
        this.plugin = plugin;
        this.cooldownService = cooldownService;
        this.databaseSupplier = databaseSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatPluginSettings settings = this.plugin.getSettings();

        if (settings.cooldown().enabled() && !player.hasPermission("advchat.bypasscooldown")) {
            long remaining = this.cooldownService.remainingMillis(player.getUniqueId());
            if (remaining > 0L) {
                event.setCancelled(true);
                String formatted = String.format(Locale.US, "%.1f", remaining / 1000.0D);
                String raw = settings.cooldown().message().replace("%number%", formatted);
                player.sendMessage(this.miniMessage.deserialize(raw));
                return;
            }
        }
        this.cooldownService.mark(player.getUniqueId());

        String format = settings.chatFormat();
        format = format.replace("%player%", player.getName());
        format = format.replace("%displayname%", PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        format = format.replace("%world%", player.getWorld().getName());
        format = format.replace("%server%", settings.serverName());

        if (this.plugin.isPapiEnabled()) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        String processedMessage = processMessage(player, rawMessage);

        format = format.replace("%message%", processedMessage);

        Component rendered = this.miniMessage.deserialize(format);

        event.setCancelled(true);
        for (Audience viewer : event.viewers()) {
            viewer.sendMessage(rendered);
        }

        if (settings.chatSync()) {
            ChatDatabase database = this.databaseSupplier.get();
            if (database != null) {
                database.insertAsync(player.getUniqueId(), player.getName(), format);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.cooldownService.cleanupPlayer(event.getPlayer().getUniqueId());
    }

    private String processMessage(Player player, String message) {
        if (player.hasPermission("advchat.minimessage")) {
            return message;
        }

        if (player.hasPermission("advchat.colorcodes")) {
            Component legacy = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            return this.miniMessage.serialize(legacy);
        }

        return this.miniMessage.escapeTags(message);
    }
}
