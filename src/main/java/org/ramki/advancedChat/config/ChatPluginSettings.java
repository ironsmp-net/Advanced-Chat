package org.ramki.advancedChat.config;

import org.bukkit.configuration.file.FileConfiguration;

public record ChatPluginSettings(
        String chatFormat,
        String serverName,
        boolean chatSync,
        CooldownSection cooldown,
        DatabaseSection database,
        MuteSection mute
) {
    public record CooldownSection(boolean enabled, double seconds, String message) {
        public long millis() {
            return (long) (this.seconds * 1000.0D);
        }
    }

    public record DatabaseSection(
            boolean enabled,
            String host,
            int port,
            String database,
            String username,
            String password,
            String table,
            String muteTable,
            long pollIntervalTicks
    ) {}

    public record MuteSection(
            String chatBlockedMessage,
            long syncIntervalTicks
    ) {}

    public static ChatPluginSettings load(FileConfiguration config) {
        CooldownSection cooldown = new CooldownSection(
                config.getBoolean("cooldown.enabled", true),
                config.getDouble("cooldown.seconds", 1.5D),
                config.getString("cooldown.message",
                        "<dark_red>⚠ <dark_grey>You must wait %number% seconds before chatting again.")
        );

        DatabaseSection database = new DatabaseSection(
                config.getBoolean("database.enabled", false),
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "advancedchat"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""),
                config.getString("database.table", "advchat_messages"),
                config.getString("database.mute-table", "advchat_mutes"),
                config.getLong("database.poll-interval-ticks", 10L)
        );

        MuteSection mute = new MuteSection(
                config.getString("mute.chat-blocked-message",
                        "<red>You are muted (%days% Days, %hours% Hours, %minutes% Minutes)!\n<red>Reason: <yellow>(%reason%)"),
                config.getLong("mute.sync-interval-ticks", 20L)
        );

        return new ChatPluginSettings(
                config.getString("chat-format", "%player%: %message%"),
                config.getString("server-name", "server"),
                config.getBoolean("chat-sync", true),
                cooldown,
                database,
                mute
        );
    }
}
