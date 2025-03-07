package dev.glicio.database;

import dev.glicio.GPlayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class PlayerDb {

    public static GPlayer getDbPlayer(String uuid, Connection connection) throws SQLException {
            String currentPlayerQuery = "SELECT * FROM player WHERE uuid = ?";

            var currentPlayer = connection.prepareStatement(currentPlayerQuery);
            currentPlayer.setString(1, uuid);
            currentPlayer.execute();
            var resultSet = currentPlayer.getResultSet();
            if (resultSet.next()) {
                return new GPlayer(
                        resultSet.getString("player_name"),
                        resultSet.getString("uuid"),
                        resultSet.getTimestamp("last_login"),
                        resultSet.getInt("balance")
                );
            }
            return null;
    }

    public static void saveDbPlayer(GPlayer player, Connection connection) throws SQLException {
            String insertPlayerQuery = "INSERT INTO player (uuid, player_name, last_login, balance) VALUES (?, ?, ?, ?)";
            var insertPlayer = connection.prepareStatement(insertPlayerQuery);
            insertPlayer.setString(1, player.getUuid());
            insertPlayer.setString(2, player.getName());
            insertPlayer.setTimestamp(3, Timestamp.from(Instant.now()));
            insertPlayer.setInt(4, player.getBalance());
            insertPlayer.execute();
    }

    public static void updateLastLogin(String uuid, Connection connection) throws SQLException {
            String updatePlayerQuery = "UPDATE player SET last_login = ? WHERE uuid = ?";
            var updatePlayer = connection.prepareStatement(updatePlayerQuery);
            updatePlayer.setTimestamp(1, Timestamp.from(Instant.now()));
            updatePlayer.setString(2, uuid);
            updatePlayer.execute();
    }
    
    public static void updateBalance(String uuid, int amount, Connection connection) throws SQLException {
            String updateBalanceQuery = "UPDATE player SET balance = balance + ? WHERE uuid = ?";
            var updateBalance = connection.prepareStatement(updateBalanceQuery);
            updateBalance.setInt(1, amount);
            updateBalance.setString(2, uuid);
            updateBalance.execute();
    }
    
    public static int getBalance(String uuid, Connection connection) throws SQLException {
            String getBalanceQuery = "SELECT balance FROM player WHERE uuid = ?";
            var getBalance = connection.prepareStatement(getBalanceQuery);
            getBalance.setString(1, uuid);
            getBalance.execute();
            var resultSet = getBalance.getResultSet();
            if (resultSet.next()) {
                return resultSet.getInt("balance");
            }
            return 0;
    }
}
