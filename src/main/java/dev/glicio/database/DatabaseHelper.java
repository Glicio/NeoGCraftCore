package dev.glicio.database;

import dev.glicio.Config;

import java.sql.*;

import com.mojang.logging.LogUtils;
import dev.glicio.WarpCoord;
import org.slf4j.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static HikariDataSource dataSource;

    public static void Init() {
        LOGGER.info("============================================================");
        LOGGER.info("Initializing database connection to: {}", Config.databaseUrl);
        LOGGER.info("Using database user: {}", Config.databaseUser);
        LOGGER.info("Using database password: {}", Config.databasePassword);
        LOGGER.info("============================================================");

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(Config.databaseUrl);
            config.setUsername(Config.databaseUser);
            config.setPassword(Config.databasePassword);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(20000);
            
            dataSource = new HikariDataSource(config);

            // Define SQL statements
            String createPlayerTableSQL = """
                    CREATE TABLE IF NOT EXISTS player (
                        uuid TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL DEFAULT 0,
                        shops_created INTEGER NOT NULL DEFAULT 0,
                        player_name TEXT,
                        last_login TIMESTAMP DEFAULT NOW()
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

            //warps
            String createDefaultSpawnSQL = """
                    INSERT INTO coords (id, x, y, z, world, friendly_name) VALUES ('spawn', 0, 0, 0, 'minecraft:overworld', 'Spawn');
                    """;

            String getDefaultSpawnSQL = """
                    SELECT * FROM coords WHERE id = 'spawn';
                    """;

            //shops
            String createShopTableSQL = """
                    CREATE TABLE IF NOT EXISTS shop (
                        id SERIAL PRIMARY KEY,
                        posX INTEGER NOT NULL,
                        posY INTEGER NOT NULL,
                        posZ INTEGER NOT NULL,
                        owner TEXT NOT NULL,
                        value INTEGER NOT NULL,
                        item TEXT NOT NULL,
                        is_admin BOOLEAN NOT NULL DEFAULT FALSE,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        shop_type TEXT NOT NULL
                    );
                    """;

            // Execute SQL statements
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(createPlayerTableSQL);
                LOGGER.info("Verified or created 'player' table.");

                statement.execute(createCoordsTableSQL);
                LOGGER.info("Verified or created 'coords' table.");

                statement.execute(createShopTableSQL);
                LOGGER.info("Verified or created 'shop' table.");

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

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
