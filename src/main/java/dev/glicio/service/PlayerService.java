package dev.glicio.service;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.PlayerDao;
import dev.glicio.model.GPlayer;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, GPlayer> players = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startFlushScheduler() {
        scheduler.scheduleAtFixedRate(PlayerService::flushDirty, 5, 5, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        scheduler.shutdown();
        flushAll();
    }

    public static void add(String uuid, GPlayer player) {
        players.put(uuid, player);
    }

    public static void remove(String uuid) {
        GPlayer player = players.remove(uuid);
        if (player != null && player.isDirty()) {
            flush(player);
        }
    }

    public static GPlayer get(String uuid) {
        return players.get(uuid);
    }

    public static Collection<GPlayer> getAll() {
        return players.values();
    }

    public static void flush(GPlayer player) {
        try (Connection con = DatabaseHelper.getConnection()) {
            PlayerDao.saveBalance(player.getUuid(), player.getBalance(), con);
            player.clearDirty();
        } catch (Exception e) {
            LOGGER.error("Failed to save balance for {}: {}", player.getName(), e.getMessage());
        }
    }

    public static void flushDirty() {
        players.values().stream().filter(GPlayer::isDirty).forEach(PlayerService::flush);
    }

    public static void flushAll() {
        players.values().forEach(PlayerService::flush);
    }

    // Synchronized in-memory deduct for online players, atomic DB deduct for offline
    public static boolean deductBalance(String uuid, int amount) {
        GPlayer player = get(uuid);
        if (player != null) {
            synchronized (player) {
                if (player.getBalance() < amount) return false;
                player.addBalance(-amount);
            }
            return true;
        }
        try (Connection con = DatabaseHelper.getConnection()) {
            return PlayerDao.deductBalance(uuid, amount, con);
        } catch (Exception e) {
            LOGGER.error("Failed to deduct balance for offline player {}: {}", uuid, e.getMessage());
            return false;
        }
    }

    // In-memory add for online players, DB add for offline
    public static void addBalance(String uuid, int amount) {
        GPlayer player = get(uuid);
        if (player != null) {
            player.addBalance(amount);
            return;
        }
        try (Connection con = DatabaseHelper.getConnection()) {
            PlayerDao.addBalance(uuid, amount, con);
        } catch (Exception e) {
            LOGGER.error("Failed to add balance for offline player {}: {}", uuid, e.getMessage());
        }
    }
}
