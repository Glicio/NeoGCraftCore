package dev.glicio.service;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.WarpDao;
import dev.glicio.model.WarpCoord;
import org.slf4j.Logger;

import java.sql.Connection;

public class WarpService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static WarpCoord spawn;

    public static void loadSpawn() {
        try (Connection con = DatabaseHelper.getConnection()) {
            spawn = WarpDao.getSpawn(con);
            if (spawn == null) {
                WarpDao.insertDefaultSpawn(con);
                spawn = new WarpCoord("spawn", 0, 0, 0, "minecraft:overworld", "Spawn");
                LOGGER.info("Default spawn created at (0, 0, 0)");
            } else {
                LOGGER.info("Spawn loaded: {} at ({}, {}, {})", spawn.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load spawn: {}", e.getMessage());
        }
    }

    public static WarpCoord getSpawn() {
        return spawn;
    }

    public static void setSpawn(WarpCoord coord) {
        try (Connection con = DatabaseHelper.getConnection()) {
            WarpDao.updateSpawn(coord, con);
            spawn = coord;
        } catch (Exception e) {
            LOGGER.error("Failed to save spawn: {}", e.getMessage());
        }
    }
}
