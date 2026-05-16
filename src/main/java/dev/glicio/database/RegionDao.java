package dev.glicio.database;

import dev.glicio.model.Region;
import net.minecraft.world.phys.Vec2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegionDao {

    public static void createTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS region (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
                    top_left_x INTEGER NOT NULL,
                    top_left_y INTEGER NOT NULL,
                    bottom_right_x INTEGER NOT NULL,
                    bottom_right_y INTEGER NOT NULL,
                    owner_id TEXT,
                    owner_name TEXT
                )
            """);
            stmt.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'region' AND column_name = 'owner_id') THEN
                        ALTER TABLE region ADD COLUMN owner_id TEXT;
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'region' AND column_name = 'owner_name') THEN
                        ALTER TABLE region ADD COLUMN owner_name TEXT;
                    END IF;
                END $$
            """);
        }
    }

    public static List<Region> loadAll(Connection connection) throws SQLException {
        List<Region> regions = new ArrayList<>();
        try (var stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM region")) {
            while (rs.next()) {
                Region region = new Region(
                        rs.getString("name"),
                        new Vec2(rs.getInt("top_left_x"), rs.getInt("top_left_y")),
                        new Vec2(rs.getInt("bottom_right_x"), rs.getInt("bottom_right_y")),
                        rs.getString("owner_id"),
                        rs.getString("owner_name")
                );
                region.setId(rs.getInt("id"));
                regions.add(region);
            }
        }
        return regions;
    }

    public static int save(Region region, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "INSERT INTO region (name, top_left_x, top_left_y, bottom_right_x, bottom_right_y, owner_id, owner_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            stmt.setString(1, region.getName());
            stmt.setInt(2, (int) region.getBounds().getTopLeft().x);
            stmt.setInt(3, (int) region.getBounds().getTopLeft().y);
            stmt.setInt(4, (int) region.getBounds().getBottomRight().x);
            stmt.setInt(5, (int) region.getBounds().getBottomRight().y);
            stmt.setString(6, region.getOwnerId());
            stmt.setString(7, region.getOwnerName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public static void delete(int regionId, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement("DELETE FROM region WHERE id = ?")) {
            stmt.setInt(1, regionId);
            stmt.execute();
        }
    }
}
