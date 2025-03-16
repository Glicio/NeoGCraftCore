package dev.glicio.regions;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.world.phys.Vec2;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class RegionHandler {
    private static Logger LOGGER = LogUtils.getLogger();
    private static Map<Integer, Region> regions = new HashMap<>();
    /**
     * Initialize the region table
     */
    public static void InitTable(Connection con) {
        String createRegionTableSQL = """
                CREATE TABLE IF NOT EXISTS region (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
                    top_left_x INTEGER NOT NULL,
                    top_left_y INTEGER NOT NULL,
                    bottom_right_x INTEGER NOT NULL,
                    bottom_right_y INTEGER NOT NULL,
                    owner_id TEXT,
                    owner_name TEXT
                );
                """;
                
        // Migration SQL to add owner_id and owner_name columns if they don't exist
        String addOwnerColumnsSQL = """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'region'
                        AND column_name = 'owner_id'
                    ) THEN
                        ALTER TABLE region ADD COLUMN owner_id TEXT;
                    END IF;

                    IF NOT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'region'
                        AND column_name = 'owner_name'
                    ) THEN
                        ALTER TABLE region ADD COLUMN owner_name TEXT;
                    END IF;
                END $$;
                """;
                
        try {
            // Create table if it doesn't exist
            con.createStatement().execute(createRegionTableSQL);
            
            // Add owner columns if they don't exist
            con.createStatement().execute(addOwnerColumnsSQL);
            
            LOGGER.info("Created/updated region table successfully");
            LoadRegions(con);
        } catch (Exception e) {
            LOGGER.error("Failed to create/update region table: {}", e.getMessage());
        }
    }

    /**
     * Load all regions from the database
     * @param con The database connection
     */
    public static void LoadRegions(Connection con) {
        String getAllRegionsSQL = "SELECT * FROM region";
        try {
            var resultSet = con.createStatement().executeQuery(getAllRegionsSQL);
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int topLeftX = resultSet.getInt("top_left_x");
                int topLeftY = resultSet.getInt("top_left_y");
                int bottomRightX = resultSet.getInt("bottom_right_x");
                int bottomRightY = resultSet.getInt("bottom_right_y");
                String ownerId = resultSet.getString("owner_id");
                String ownerName = resultSet.getString("owner_name");
                
                Region region = new Region(name, new Vec2(topLeftX, topLeftY), new Vec2(bottomRightX, bottomRightY), ownerId, ownerName);
                region.setId(id);
                regions.put(id, region);
            }
            LOGGER.info("Loaded {} regions", regions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load regions: {}", e.getMessage());
        }
    }

    public static Region getRegion(int id) {
        return regions.get(id);
    }

    public static void saveRegion(Region region) {
        String insertRegionSQL = "INSERT INTO region (name, top_left_x, top_left_y, bottom_right_x, bottom_right_y, owner_id, owner_name) VALUES (?, ?, ?, ?, ?, ?, ?) returning id";
        try {
            Connection con = DatabaseHelper.getConnection();
            var insertRegion = con.prepareStatement(insertRegionSQL);
            insertRegion.setString(1, region.getName());
            insertRegion.setInt(2, (int) region.getBounds().getTopLeft().x);
            insertRegion.setInt(3, (int) region.getBounds().getTopLeft().y);
            insertRegion.setInt(4, (int) region.getBounds().getBottomRight().x);
            insertRegion.setInt(5, (int) region.getBounds().getBottomRight().y);
            insertRegion.setString(6, region.getOwnerId());
            insertRegion.setString(7, region.getOwnerName());
            ResultSet resultSet = insertRegion.executeQuery();
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                region.setId(id);
                regions.put(id, region);
                LOGGER.info("Saved region: {}", region.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save region: {}", e.getMessage());
        }
    }

    public static void deleteRegion(Region region) {
        String deleteRegionSQL = "DELETE FROM region WHERE id = ?";
        try {
            Connection con = DatabaseHelper.getConnection();
            var deleteRegion = con.prepareStatement(deleteRegionSQL);
            deleteRegion.setInt(1, region.getId());
            deleteRegion.execute();
            regions.remove(region.getId());
            LOGGER.info("Deleted region: {}", region);
        } catch (Exception e) {
            LOGGER.error("Failed to delete region: {}", e.getMessage());
        }
    }

    /**
     * Get all regions currently loaded in the system
     * @return A copy of the regions map to prevent direct modification
     */
    public static Map<Integer, Region> getAllRegions() {
        return new HashMap<>(regions);
    }

}
