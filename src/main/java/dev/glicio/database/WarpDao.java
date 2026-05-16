package dev.glicio.database;

import dev.glicio.model.WarpCoord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WarpDao {

    public static void createTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS coords (
                    id TEXT PRIMARY KEY NOT NULL UNIQUE,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    world TEXT NOT NULL,
                    friendly_name TEXT NOT NULL
                )
            """);
        }
    }

    public static WarpCoord getSpawn(Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement("SELECT * FROM coords WHERE id = 'spawn'");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new WarpCoord(
                        rs.getString("id"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getString("world"),
                        rs.getString("friendly_name")
                );
            }
        }
        return null;
    }

    public static void insertDefaultSpawn(Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "INSERT INTO coords (id, x, y, z, world, friendly_name) VALUES ('spawn', 0, 0, 0, 'minecraft:overworld', 'Spawn')")) {
            stmt.execute();
        }
    }

    public static void updateSpawn(WarpCoord coord, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "UPDATE coords SET world = ?, x = ?, y = ?, z = ? WHERE id = 'spawn'")) {
            stmt.setString(1, coord.getWorld());
            stmt.setInt(2, coord.getX());
            stmt.setInt(3, coord.getY());
            stmt.setInt(4, coord.getZ());
            stmt.execute();
        }
    }
}
