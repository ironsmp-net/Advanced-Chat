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

public final class ChatListener implements Listener {

    private final AdvancedChat plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ChatListener(AdvancedChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        String format = plugin.getConfig().getString("chat-format", "%player%: %message%");
        String serverName = plugin.getConfig().getString("server-name", "server");

        format = format.replace("%player%", player.getName());
        format = format.replace("%displayname%", PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        format = format.replace("%world%", player.getWorld().getName());
        format = format.replace("%server%", serverName);

        if (plugin.isPapiEnabled()) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        String processedMessage = processMessage(player, rawMessage);

        format = format.replace("%message%", processedMessage);

        Component rendered = miniMessage.deserialize(format);

        event.renderer((source, sourceDisplayName, message, audience) -> rendered);
    }

    private String processMessage(Player player, String message) {
        if (player.hasPermission("advchat.minimessage")) {
            return message;
        }

        if (player.hasPermission("advchat.colorcodes")) {
            Component legacy = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            return miniMessage.serialize(legacy);
        }

        return miniMessage.escapeTags(message);
    }
}
