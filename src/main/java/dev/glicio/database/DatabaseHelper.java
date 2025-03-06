package dev.glicio.database;

import dev.glicio.Config;

import java.sql.*;

import com.mojang.logging.LogUtils;
import dev.glicio.WarpCoord;
import org.slf4j.Logger;

public class DatabaseHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void Init() {
        LOGGER.info("============================================================");
        LOGGER.info("Initializing database connection to: {}", Config.databaseUrl);
        LOGGER.info("Using database user: {}", Config.databaseUser);
        LOGGER.info("Using database password: {}", Config.databasePassword);
        LOGGER.info("============================================================");

        // Initialize database connection
        try (Connection connection = DriverManager.getConnection(Config.databaseUrl, Config.databaseUser, Config.databasePassword)) {
            LOGGER.info("Successfully connected to the database.");

            // Define SQL statements
            String createRankTableSQL = """
                    CREATE TABLE IF NOT EXISTS player_rank (
                        id SERIAL PRIMARY KEY,
                        players_count INTEGER NOT NULL,
                        rank_name TEXT NOT NULL,
                        rank_color TEXT NOT NULL,
                        rank_prefix TEXT NOT NULL,
                        rank_description TEXT NOT NULL,
                        rank_power INTEGER NOT NULL DEFAULT 0
                    );
                    """;

            String createPlayerTableSQL = """
                    CREATE TABLE IF NOT EXISTS player (
                        uuid TEXT PRIMARY KEY,
                        player_name TEXT,
                        rank_id INTEGER,
                        last_login TIMESTAMP DEFAULT NOW(),
                    
                        FOREIGN KEY(rank_id) REFERENCES player_rank(id)
                    );
                    """;
            String createCoordsTableSQL = """
                    CREATE TABLE IF NOT EXISTS coords (
                        id TEXT PRIMARY KEY NOT NULL UNIQUE,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        world TEXT NOT NULL,
                        friendly_name TEXT NOT NULL
                    );
                    """;

            String createDefaultSpawnSQL = """
                    INSERT INTO coords (id, x, y, z, world, friendly_name) VALUES ('spawn', 0, 0, 0, 'minecraft:overworld', 'Spawn');
                    """;

            String getDefaultSpawnSQL = """
                    SELECT * FROM coords WHERE id = 'spawn';
                    """;

            // Execute SQL statements
            try (Statement statement = connection.createStatement()) {
                statement.execute(createRankTableSQL);
                LOGGER.info("Verified or created 'player_rank' table.");

                statement.execute(createPlayerTableSQL);
                LOGGER.info("Verified or created 'player' table.");

                statement.execute(createCoordsTableSQL);
                LOGGER.info("Verified or created 'coords' table.");

                //get current spawn
                statement.execute(getDefaultSpawnSQL);
                ResultSet resultSet = statement.getResultSet();
                if(resultSet.next()) {
                    Config.spawnCoord = new WarpCoord(resultSet.getString("id"), resultSet.getInt("x"), resultSet.getInt("y"), resultSet.getInt("z"), resultSet.getString("world"), resultSet.getString("friendly_name"));
                } else {
                    statement.execute(createDefaultSpawnSQL);
                    Config.spawnCoord = new WarpCoord("spawn", 0, 0, 0, "minecraft:overworld", "Spawn");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Database initialization failed: {}", e.getMessage());
        }
    }


    public DatabaseHelper() {
        Init();
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(Config.databaseUrl, Config.databaseUser, Config.databasePassword);
        } catch (SQLException e) {
            LOGGER.error("Failed to get database connection: {}", e.getMessage());
            return null;
        }
    }

}
