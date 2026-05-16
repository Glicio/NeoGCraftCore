package dev.glicio.database;

import dev.glicio.model.GPlayer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class PlayerDao {

    public static void createTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player (
                    uuid TEXT PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    shops_created INTEGER NOT NULL DEFAULT 0,
                    player_name TEXT,
                    last_login TIMESTAMP DEFAULT NOW()
                )
            """);
        }
    }

    public static GPlayer getDbPlayer(String uuid, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement("SELECT * FROM player WHERE uuid = ?")) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new GPlayer(
                            rs.getString("player_name"),
                            rs.getString("uuid"),
                            rs.getTimestamp("last_login"),
                            rs.getInt("balance")
                    );
                }
            }
        }
        return null;
    }

    public static void saveDbPlayer(GPlayer player, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "INSERT INTO player (uuid, player_name, last_login, balance) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, player.getUuid());
            stmt.setString(2, player.getName());
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setInt(4, player.getBalance());
            stmt.execute();
        }
    }

    public static void updateLastLogin(String uuid, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "UPDATE player SET last_login = ? WHERE uuid = ?")) {
            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, uuid);
            stmt.execute();
        }
    }

    public static void saveBalance(String uuid, int balance, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "UPDATE player SET balance = ? WHERE uuid = ?")) {
            stmt.setInt(1, balance);
            stmt.setString(2, uuid);
            stmt.execute();
        }
    }

    // Atomic check-and-deduct for offline players
    public static boolean deductBalance(String uuid, int amount, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "UPDATE player SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid);
            stmt.setInt(3, amount);
            return stmt.executeUpdate() > 0;
        }
    }

    public static void addBalance(String uuid, int amount, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "UPDATE player SET balance = balance + ? WHERE uuid = ?")) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid);
            stmt.execute();
        }
    }
}
