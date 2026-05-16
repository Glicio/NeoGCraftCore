package dev.glicio.service;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.RegionDao;
import dev.glicio.model.Region;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegionService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Integer, Region> regions = new ConcurrentHashMap<>();

    public static void loadRegions() {
        try (Connection con = DatabaseHelper.getConnection()) {
            for (Region region : RegionDao.loadAll(con)) {
                regions.put(region.getId(), region);
            }
            LOGGER.info("Loaded {} regions", regions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load regions: {}", e.getMessage());
        }
    }

    public static Region get(int id) {
        return regions.get(id);
    }

    public static Map<Integer, Region> getAll() {
        return Collections.unmodifiableMap(regions);
    }

    public static void save(Region region) {
        try (Connection con = DatabaseHelper.getConnection()) {
            int id = RegionDao.save(region, con);
            if (id != -1) {
                region.setId(id);
                regions.put(id, region);
                LOGGER.info("Saved region: {}", region.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save region {}: {}", region.getName(), e.getMessage());
        }
    }

    public static void delete(Region region) {
        try (Connection con = DatabaseHelper.getConnection()) {
            RegionDao.delete(region.getId(), con);
            regions.remove(region.getId());
            LOGGER.info("Deleted region: {}", region.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to delete region {}: {}", region.getName(), e.getMessage());
        }
    }
}
