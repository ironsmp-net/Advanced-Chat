package org.ramki.advancedChat.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.config.ChatPluginSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChatDatabase {

    public record StoredMessage(
            long id,
            String serverName,
            UUID senderUuid,
            String senderName,
            String renderedMiniMessage
    ) {}

    private final AdvancedChat plugin;
    private final HikariDataSource dataSource;
    private final ExecutorService executor;
    private final Logger logger;
    private final String table;
    private final MuteRepository muteRepository;

    public ChatDatabase(AdvancedChat plugin, ExecutorService executor, Logger logger) {
        this.plugin = plugin;
        this.executor = executor;
        this.logger = logger;
        ChatPluginSettings.DatabaseSection db = plugin.getSettings().database();
        this.table = db.table();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format(
                "jdbc:mariadb://%s:%d/%s?useSsl=false&autoReconnect=true",
                db.host(), db.port(), db.database()));
        hikariConfig.setUsername(db.username());
        hikariConfig.setPassword(db.password());
        hikariConfig.setPoolName("advchat-pool");
        hikariConfig.setMaximumPoolSize(6);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(5000L);
        hikariConfig.setLeakDetectionThreshold(15000L);

        this.dataSource = new HikariDataSource(hikariConfig);

        try {
            initSchema();
        } catch (SQLException ex) {
            this.dataSource.close();
            throw new IllegalStateException("Failed to create chat sync table", ex);
        }

        try {
            this.muteRepository = new MuteRepository(this.dataSource, this.executor, this.logger, db.muteTable());
        } catch (RuntimeException ex) {
            this.dataSource.close();
            throw ex;
        }
    }

    public MuteRepository getMuteRepository() {
        return this.muteRepository;
    }

    private void initSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + this.table + " ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "server_name VARCHAR(64) NOT NULL,"
                + "sender_uuid VARCHAR(36) NOT NULL,"
                + "sender_name VARCHAR(64) NOT NULL,"
                + "rendered_minimessage TEXT NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "INDEX idx_id (id)"
                + ")";
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public long currentMaxId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM " + this.table;
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            this.logger.log(Level.WARNING, "Failed to read current max chat id", ex);
            return 0L;
        }
    }

    public CompletableFuture<Void> insertAsync(UUID senderUuid, String senderName, String renderedMiniMessage) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + this.table
                    + " (server_name, sender_uuid, sender_name, rendered_minimessage, created_at)"
                    + " VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = this.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, this.plugin.getSettings().serverName());
                stmt.setString(2, senderUuid.toString());
                stmt.setString(3, senderName);
                stmt.setString(4, renderedMiniMessage);
                stmt.setLong(5, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                this.logger.log(Level.WARNING,
                        String.format("Failed to insert chat message for %s", senderUuid), ex);
            }
        }, this.executor);
    }

    public List<StoredMessage> pollSince(long lastSeenId) {
        String sql = "SELECT id, server_name, sender_uuid, sender_name, rendered_minimessage FROM "
                + this.table + " WHERE id > ? AND server_name != ? ORDER BY id ASC LIMIT 200";
        List<StoredMessage> result = new ArrayList<>();
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, lastSeenId);
            stmt.setString(2, this.plugin.getSettings().serverName());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new StoredMessage(
                            rs.getLong("id"),
                            rs.getString("server_name"),
                            UUID.fromString(rs.getString("sender_uuid")),
                            rs.getString("sender_name"),
                            rs.getString("rendered_minimessage")
                    ));
                }
            }
        } catch (SQLException ex) {
            this.logger.log(Level.WARNING, "Failed to poll chat messages", ex);
        }
        return result;
    }

    public void shutdown() {
        if (!this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }
}