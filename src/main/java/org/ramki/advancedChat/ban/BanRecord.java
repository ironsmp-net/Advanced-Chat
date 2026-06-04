package org.ramki.advancedChat.ban;

import java.util.UUID;

public record BanRecord(UUID uuid, String playerName, long expiresAt, String reason, String bannerName, long updatedAt) {

    public boolean isExpired(long nowMillis) {
        return nowMillis >= this.expiresAt;
    }

    public long remainingMillis(long nowMillis) {
        long remaining = this.expiresAt - nowMillis;
        return remaining > 0L ? remaining : 0L;
    }
}