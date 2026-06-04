package org.ramki.advancedChat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.ramki.advancedChat.ban.BanRecord;
import org.ramki.advancedChat.service.BanService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class BanListener implements Listener {

    private final AdvancedChat plugin;
    private final BanService banService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public BanListener(AdvancedChat plugin, BanService banService) {
        this.plugin = plugin;
        this.banService = banService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        Optional<BanRecord> banOpt = this.banService.getActive(event.getUniqueId());
        if (banOpt.isEmpty()) return;

        BanRecord ban = banOpt.get();
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                buildKickMessage(ban, event.getName()));
    }

    private Component buildKickMessage(BanRecord ban, String playerName) {
        long remaining = ban.remainingMillis(System.currentTimeMillis());
        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60L;
        if (days == 0L && hours == 0L && minutes == 0L && remaining > 0L) minutes = 1L;

        String raw = this.plugin.getSettings().ban().kickMessage()
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours))
                .replace("%minutes%", String.valueOf(minutes))
                .replace("%reason%", ban.reason())
                .replace("%banner%", ban.bannerName())
                .replace("%player%", playerName);
        return this.miniMessage.deserialize(raw);
    }
}