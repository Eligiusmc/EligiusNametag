package com.makrozai.eligiusnametag.adapter.database;

import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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
    private Connection sqliteConnection;
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
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            sqliteConnection = DriverManager.getConnection(url);
            
            try (Statement statement = sqliteConnection.createStatement()) {
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
        try {
            if (sqliteConnection != null && !sqliteConnection.isClosed()) {
                sqliteConnection.close();
            }
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                hikariDataSource.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        if (isMySQL) {
            return hikariDataSource.getConnection();
        } else {
            return sqliteConnection;
        }
    }

    @Override
    public boolean getPlayerViewSelf(UUID uuid) {
        String sql = "SELECT view_self FROM player_settings WHERE uuid = ?;";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("view_self");
                    }
                }
            } finally {
                if (isMySQL && conn != null) {
                    conn.close();
                }
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setPlayerViewSelf(UUID uuid, boolean viewSelf) {
        String sql = isMySQL 
            ? "INSERT INTO player_settings (uuid, view_self) VALUES (?, ?) ON DUPLICATE KEY UPDATE view_self = VALUES(view_self);"
            : "INSERT INTO player_settings (uuid, view_self) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET view_self = excluded.view_self;";
            
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setBoolean(2, viewSelf);
                pstmt.executeUpdate();
            } finally {
                if (isMySQL && conn != null) {
                    conn.close();
                }
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<UUID> getAllPlayersWithViewSelf() {
        Set<UUID> players = new HashSet<>();
        String sql = isMySQL ? "SELECT uuid FROM player_settings WHERE view_self = TRUE;" 
                             : "SELECT uuid FROM player_settings WHERE view_self = 1;";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        players.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException ignored) {}
                }
            } finally {
                if (isMySQL && conn != null) {
                    conn.close();
                }
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
        return players;
    }
}
