package com.makrozai.eligiusnametag.adapter.database;

import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.makrozai.eligiusnametag.domain.port.ConfigPort;

public class DatabaseAdapter implements DatabasePort {
    private final JavaPlugin plugin;
    private final ConfigPort config;
    private HikariDataSource hikariDataSource;
    private boolean isMySQL;

    public DatabaseAdapter(JavaPlugin plugin, ConfigPort config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean initialize() {
        this.isMySQL = "mysql".equalsIgnoreCase(config.getDatabaseType());
        if (isMySQL) {
            return setupMySQL();
        } else {
            return setupSQLite();
        }
    }

    private boolean setupMySQL() {
        try {
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl("jdbc:mysql://" + config.getDatabaseHost() + ":" + config.getDatabasePort() + "/" + config.getDatabaseName() + "?useSSL=false&autoReconnect=true");
            hc.setUsername(config.getDatabaseUsername());
            hc.setPassword(config.getDatabasePassword());
            hc.addDataSourceProperty("cachePrepStmts", "true");
            hc.addDataSourceProperty("prepStmtCacheSize", "250");
            hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hc.setMaximumPoolSize(10);

            hikariDataSource = new HikariDataSource(hc);

            try (Connection conn = hikariDataSource.getConnection();
                 Statement statement = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS player_settings (" +
                             "uuid VARCHAR(36) PRIMARY KEY," +
                             "view_self BOOLEAN NOT NULL" +
                             ");";
                statement.execute(sql);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setupSQLite() {
        try {
            // Ensure the driver is loaded before HikariCP initializes
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");

            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hc.setDriverClassName("org.sqlite.JDBC");
            // Pool size 1: SQLite is single-writer; HikariCP serializes concurrent async calls.
            hc.setMaximumPoolSize(1);
            hc.setConnectionTestQuery("SELECT 1");

            hikariDataSource = new HikariDataSource(hc);

            try (Connection conn = hikariDataSource.getConnection();
                 Statement statement = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS player_settings (" +
                             "uuid VARCHAR(36) PRIMARY KEY," +
                             "view_self BOOLEAN NOT NULL" +
                             ");";
                statement.execute(sql);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    @Override
    public boolean getPlayerViewSelf(UUID uuid) {
        String sql = "SELECT view_self FROM player_settings WHERE uuid = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("view_self");
                }
            }
        } catch (SQLException | NullPointerException e) {
            plugin.getLogger().severe("Failed to query player view_self: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void setPlayerViewSelf(UUID uuid, boolean viewSelf) {
        String sql = isMySQL
            ? "INSERT INTO player_settings (uuid, view_self) VALUES (?, ?) ON DUPLICATE KEY UPDATE view_self = VALUES(view_self);"
            : "INSERT INTO player_settings (uuid, view_self) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET view_self = excluded.view_self;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setBoolean(2, viewSelf);
            pstmt.executeUpdate();
        } catch (SQLException | NullPointerException e) {
            plugin.getLogger().severe("Failed to update player view_self: " + e.getMessage());
        }
    }

    @Override
    public Set<UUID> getAllPlayersWithViewSelf() {
        Set<UUID> players = new HashSet<>();
        String sql = isMySQL ? "SELECT uuid FROM player_settings WHERE view_self = TRUE;"
                             : "SELECT uuid FROM player_settings WHERE view_self = 1;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                try {
                    players.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException | NullPointerException e) {
            plugin.getLogger().severe("Failed to fetch all view_self players: " + e.getMessage());
        }
        return players;
    }
}
