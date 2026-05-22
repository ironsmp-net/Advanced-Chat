package org.ramki.advancedChat.service;

import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.config.ChatPluginSettings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownService {

    private final Map<UUID, Long> lastChatMillis = new ConcurrentHashMap<>();
    private final AdvancedChat plugin;

    public CooldownService(AdvancedChat plugin) {
        this.plugin = plugin;
    }

    public long remainingMillis(UUID uuid) {
        ChatPluginSettings.CooldownSection cooldown = this.plugin.getSettings().cooldown();
        if (!cooldown.enabled()) return 0L;

        Long last = this.lastChatMillis.get(uuid);
        if (last == null) return 0L;

        long elapsed = System.currentTimeMillis() - last;
        long window = cooldown.millis();
        return elapsed >= window ? 0L : window - elapsed;
    }

    public void mark(UUID uuid) {
        this.lastChatMillis.put(uuid, System.currentTimeMillis());
    }

    public void cleanupPlayer(UUID uuid) {
        this.lastChatMillis.remove(uuid);
    }

    public void clear() {
        this.lastChatMillis.clear();
    }
}