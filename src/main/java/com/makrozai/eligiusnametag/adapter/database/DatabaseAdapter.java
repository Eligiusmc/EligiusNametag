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

public class DatabaseAdapter implements DatabasePort {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS player_settings (" +
                             "uuid VARCHAR(36) PRIMARY KEY," +
                             "view_self BOOLEAN NOT NULL" +
                             ");";
                statement.execute(sql);
            }
            plugin.getLogger().info("SQLite Database initialized successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite Database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getPlayerViewSelf(UUID uuid) {
        if (connection == null) return false;
        String sql = "SELECT view_self FROM player_settings WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("view_self");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setPlayerViewSelf(UUID uuid, boolean viewSelf) {
        if (connection == null) return;
        String sql = "INSERT INTO player_settings (uuid, view_self) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET view_self = excluded.view_self;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setBoolean(2, viewSelf);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<UUID> getAllPlayersWithViewSelf() {
        Set<UUID> players = new HashSet<>();
        if (connection == null) return players;
        String sql = "SELECT uuid FROM player_settings WHERE view_self = 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                try {
                    players.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
}
