package dev.glicio.database;

import com.mojang.logging.LogUtils;
import dev.glicio.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static HikariDataSource dataSource;

    public static void init() {
        LOGGER.info("Initializing database connection to: {}", Config.databaseUrl);
        LOGGER.info("Using database user: {}", Config.databaseUser);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.databaseUrl);
        config.setUsername(Config.databaseUser);
        config.setPassword(Config.databasePassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        dataSource = new HikariDataSource(config);
    }

    public static void initSchema() {
        try (Connection con = getConnection()) {
            PlayerDao.createTable(con);
            ShopDao.createTable(con);
            RegionDao.createTable(con);
            WarpDao.createTable(con);
            LOGGER.info("Database schema verified");
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize schema: {}", e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null) dataSource.close();
    }
}
