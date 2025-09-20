package com.artemis.the.gr8.playerstats.core.storage;

import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger.LogLevel;
import org.bukkit.Statistic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * MariaDB implementation of {@link WorldStatsPersistence}.
 */
public class MariaDbWorldStatsStorage implements WorldStatsPersistence {

    private final ConfigHandler.MariaDbSettings settings;
    private Connection connection;

    public MariaDbWorldStatsStorage(ConfigHandler.MariaDbSettings settings) {
        this.settings = settings;
    }

    @Override
    public void initialize() throws Exception {
        openConnection();
        createTableIfNeeded();
    }

    @Override
    public void load(WorldStatsDatabase database) throws Exception {
        ensureConnection();
        String query = "SELECT player_uuid, world, statistic, value FROM " + tableIdentifier();
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    String world = resultSet.getString("world");
                    Statistic statistic = Statistic.valueOf(resultSet.getString("statistic"));
                    int value = resultSet.getInt("value");
                    database.setStat(uuid, world, statistic, value);
                } catch (IllegalArgumentException ex) {
                    PluginLogger.logWarning("Skipping invalid statistic entry retrieved from MariaDB: " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public void save(WorldStatsDatabase database) throws Exception {
        ensureConnection();
        String deleteSql = "DELETE FROM " + tableIdentifier();
        String insertSql = "INSERT INTO " + tableIdentifier() +
                " (player_uuid, world, statistic, value) VALUES (?, ?, ?, ?)";
        try (Statement deleteStatement = connection.createStatement();
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            deleteStatement.executeUpdate(deleteSql);

            for (Map.Entry<UUID, PlayerWorldStats> entry : database.getAll().entrySet()) {
                UUID uuid = entry.getKey();
                PlayerWorldStats stats = entry.getValue();
                for (Map.Entry<String, Map<Statistic, Integer>> worldEntry : stats.getAllStats().entrySet()) {
                    String world = worldEntry.getKey();
                    for (Map.Entry<Statistic, Integer> statEntry : worldEntry.getValue().entrySet()) {
                        insertStatement.setString(1, uuid.toString());
                        insertStatement.setString(2, world);
                        insertStatement.setString(3, statEntry.getKey().name());
                        insertStatement.setInt(4, statEntry.getValue());
                        insertStatement.addBatch();
                    }
                }
            }
            insertStatement.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                connection.rollback();
            }
            throw ex;
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                PluginLogger.log(LogLevel.WARNING, "Failed to close MariaDB connection: " + ex.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    private void openConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", settings.username());
        properties.setProperty("password", settings.password());
        properties.setProperty("useSSL", String.valueOf(settings.useSsl()));
        String baseUrl = "jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/";
        Connection serverConnection = null;
        try {
            serverConnection = DriverManager.getConnection(baseUrl, properties);
            serverConnection.setAutoCommit(false);
            String databaseIdentifier = "`" + settings.database().replace("`", "") + "`";
            String createDatabaseSql = "CREATE DATABASE IF NOT EXISTS " + databaseIdentifier;
            try (Statement statement = serverConnection.createStatement()) {
                statement.executeUpdate(createDatabaseSql);
            }
            serverConnection.commit();
        } catch (SQLException ex) {
            if (serverConnection != null) {
                try {
                    serverConnection.rollback();
                } catch (SQLException rollbackEx) {
                    PluginLogger.log(LogLevel.WARNING, "Failed to rollback MariaDB database creation transaction: " + rollbackEx.getMessage());
                }
            }
            throw ex;
        } finally {
            if (serverConnection != null) {
                try {
                    serverConnection.close();
                } catch (SQLException closeEx) {
                    PluginLogger.log(LogLevel.WARNING, "Failed to close MariaDB server connection: " + closeEx.getMessage());
                }
            }
        }
        String url = baseUrl + settings.database();
        connection = DriverManager.getConnection(url, properties);
        connection.setAutoCommit(false);
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            openConnection();
            createTableIfNeeded();
        }
    }

    private void createTableIfNeeded() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableIdentifier() + " (" +
                "player_uuid CHAR(36) NOT NULL," +
                " world VARCHAR(255) NOT NULL," +
                " statistic VARCHAR(64) NOT NULL," +
                " value INT NOT NULL," +
                " PRIMARY KEY (player_uuid, world, statistic))";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            connection.commit();
        }
    }

    private String tableIdentifier() {
        return "`" + settings.table().replace("`", "") + "`";
    }
}
