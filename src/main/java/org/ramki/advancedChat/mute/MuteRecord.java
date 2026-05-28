package org.ramki.advancedChat.mute;

import java.util.UUID;

public record MuteRecord(UUID uuid, String playerName, long expiresAt, String reason, String muterName, long updatedAt) {

    public boolean isExpired(long nowMillis) {
        return nowMillis >= this.expiresAt;
    }

    public long remainingMillis(long nowMillis) {
        long remaining = this.expiresAt - nowMillis;
        return remaining > 0L ? remaining : 0L;
    }
}
