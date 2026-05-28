package org.ramki.advancedChat.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.ramki.advancedChat.mute.MuteRecord;

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

public final class MuteRepository {

    private final HikariDataSource dataSource;
    private final ExecutorService executor;
    private final Logger logger;
    private final String table;

    public MuteRepository(HikariDataSource dataSource, ExecutorService executor, Logger logger, String table) {
        this.dataSource = dataSource;
        this.executor = executor;
        this.logger = logger;
        this.table = table;
        try {
            initSchema();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create mute table", ex);
        }
    }

    private void initSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + this.table + " ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "player_name VARCHAR(64) NOT NULL,"
                + "expires_at BIGINT NOT NULL,"
                + "reason TEXT NOT NULL,"
                + "muter_name VARCHAR(64) NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "INDEX idx_expires (expires_at)"
                + ")";
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public CompletableFuture<Void> upsertAsync(MuteRecord record) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + this.table
                    + " (uuid, player_name, expires_at, reason, muter_name, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), expires_at=VALUES(expires_at),"
                    + " reason=VALUES(reason), muter_name=VALUES(muter_name), updated_at=VALUES(updated_at)";
            try (Connection conn = this.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, record.uuid().toString());
                stmt.setString(2, record.playerName());
                stmt.setLong(3, record.expiresAt());
                stmt.setString(4, record.reason());
                stmt.setString(5, record.muterName());
                stmt.setLong(6, record.updatedAt());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                this.logger.log(Level.WARNING,
                        String.format("Failed to upsert mute for %s", record.uuid()), ex);
            }
        }, this.executor);
    }

    public CompletableFuture<Void> deleteAsync(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM " + this.table + " WHERE uuid = ?";
            try (Connection conn = this.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                this.logger.log(Level.WARNING,
                        String.format("Failed to delete mute for %s", uuid), ex);
            }
        }, this.executor);
    }

    public List<MuteRecord> loadAllActive(long nowMillis) {
        String sql = "SELECT uuid, player_name, expires_at, reason, muter_name, updated_at FROM "
                + this.table + " WHERE expires_at > ?";
        List<MuteRecord> result = new ArrayList<>();
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, nowMillis);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new MuteRecord(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getLong("expires_at"),
                            rs.getString("reason"),
                            rs.getString("muter_name"),
                            rs.getLong("updated_at")
                    ));
                }
            }
        } catch (SQLException ex) {
            this.logger.log(Level.WARNING, "Failed to load active mutes", ex);
        }
        return result;
    }
}
