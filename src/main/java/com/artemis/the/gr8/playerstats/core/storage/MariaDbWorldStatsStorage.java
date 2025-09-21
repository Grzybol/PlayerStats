package com.artemis.the.gr8.playerstats.core.storage;

import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        String query = "SELECT player_uuid, player_name, world, statistic, value FROM " + tableIdentifier();
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    String playerName = resultSet.getString("player_name");
                    String world = resultSet.getString("world");
                    Statistic statistic = Statistic.valueOf(resultSet.getString("statistic"));
                    int value = resultSet.getInt("value");
                    applyLoadedStat(database, uuid, playerName, world, statistic, value);
                } catch (IllegalArgumentException ex) {
                    PluginLogger.logWarning("Skipping invalid statistic entry retrieved from MariaDB: " + ex.getMessage());
                }
            }
        }
    }

    void applyLoadedStat(WorldStatsDatabase database, UUID uuid, String playerName,
                         String world, Statistic statistic, int value) {
        PlayerWorldStats stats = database.getOrCreatePlayerStats(uuid);
        if (playerName != null && !playerName.isEmpty()) {
            database.setPlayerName(uuid, playerName);
        } else {
            cacheResolvedPlayerName(database, uuid, stats, "loading data from MariaDB");
        }
        database.setStat(uuid, world, statistic, value);
    }

    @Override
    public void save(WorldStatsDatabase database) throws Exception {
        ensureConnection();
        String deleteSql = "DELETE FROM " + tableIdentifier();
        String insertSql = "INSERT INTO " + tableIdentifier() +
                " (player_uuid, player_name, world, statistic, value) VALUES (?, ?, ?, ?, ?)";
        try (Statement deleteStatement = connection.createStatement();
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            deleteStatement.executeUpdate(deleteSql);

            for (Map.Entry<UUID, PlayerWorldStats> entry : database.getAll().entrySet()) {
                UUID uuid = entry.getKey();
                PlayerWorldStats stats = entry.getValue();
                String playerName = stats.getPlayerName();
                if (playerName == null || playerName.isEmpty()) {
                    playerName = cacheResolvedPlayerName(database, uuid, stats, "saving data to MariaDB");
                }
                for (Map.Entry<String, Map<Statistic, Integer>> worldEntry : stats.getAllStats().entrySet()) {
                    String world = worldEntry.getKey();
                    for (Map.Entry<Statistic, Integer> statEntry : worldEntry.getValue().entrySet()) {
                        insertStatement.setString(1, uuid.toString());
                        insertStatement.setString(2, playerName);
                        insertStatement.setString(3, world);
                        insertStatement.setString(4, statEntry.getKey().name());
                        insertStatement.setInt(5, statEntry.getValue());
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

    String cacheResolvedPlayerName(WorldStatsDatabase database, UUID uuid, PlayerWorldStats stats, String context) {
        if (stats == null) {
            stats = database.getOrCreatePlayerStats(uuid);
        }

        String cachedName = stats.getPlayerName();
        if (cachedName != null && !cachedName.isEmpty()) {
            return cachedName;
        }

        String resolvedName = resolvePlayerName(uuid);
        if (!resolvedName.isEmpty()) {
            stats.setPlayerName(resolvedName);
            database.setPlayerName(uuid, resolvedName);
            return resolvedName;
        }

        logWarningSafe("Unable to resolve player name for UUID " + uuid + " while " + context + ".");
        return "";
    }

    protected String resolvePlayerName(UUID uuid) {
        if (uuid == null) {
            return "";
        }

        String bukkitName = resolveNameWithBukkit(uuid);
        if (!bukkitName.isEmpty()) {
            return bukkitName;
        }

        OfflinePlayerHandler handler = OfflinePlayerHandler.getExistingInstance();
        if (handler == null && isBukkitAvailable()) {
            handler = OfflinePlayerHandler.getInstance();
        }

        if (handler != null) {
            String handlerName = handler.getKnownName(uuid);
            if (handlerName != null && !handlerName.isEmpty()) {
                return handlerName;
            }
        }

        return "";
    }

    private boolean isBukkitAvailable() {
        try {
            return Bukkit.getServer() != null;
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    private String resolveNameWithBukkit(UUID uuid) {
        if (!isBukkitAvailable()) {
            return "";
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer != null) {
                String name = offlinePlayer.getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ex) {
            logDebugSafe("Failed to resolve player name via Bukkit for UUID " + uuid + ": " + ex.getMessage());
        }
        return "";
    }

    private void logWarningSafe(String message) {
        try {
            PluginLogger.logWarning(message);
        } catch (IllegalStateException ignored) {
            // PluginLogger not initialized during unit tests
        }
    }

    private void logDebugSafe(String message) {
        try {
            PluginLogger.log(LogLevel.DEBUG, message);
        } catch (IllegalStateException ignored) {
            // PluginLogger not initialized during unit tests
        }
    }

    private void openConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("MariaDB driver not found on the classpath", ex);
        }

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
                " player_name VARCHAR(255) NOT NULL DEFAULT ''," +
                " world VARCHAR(255) NOT NULL," +
                " statistic VARCHAR(64) NOT NULL," +
                " value INT NOT NULL," +
                " PRIMARY KEY (player_uuid, world, statistic))";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            connection.commit();
        }
        ensurePlayerNameColumnExists();
    }

    private String tableIdentifier() {
        return "`" + settings.table().replace("`", "") + "`";
    }

    private void ensurePlayerNameColumnExists() throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = 'player_name'";
        String databaseName = sanitizedIdentifier(settings.database());
        String tableName = sanitizedIdentifier(settings.table());
        boolean hasColumn = false;
        try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
            checkStatement.setString(1, databaseName);
            checkStatement.setString(2, tableName);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    hasColumn = resultSet.getInt(1) > 0;
                }
            }
        }

        if (!hasColumn) {
            String alterSql = "ALTER TABLE " + tableIdentifier() +
                    " ADD COLUMN player_name VARCHAR(255) NOT NULL DEFAULT '' AFTER player_uuid";
            try (Statement alterStatement = connection.createStatement()) {
                alterStatement.executeUpdate(alterSql);
            }
            connection.commit();
        }

        enforcePlayerNameColumnDefaults();
    }

    private void enforcePlayerNameColumnDefaults() throws SQLException {
        String modifySql = "ALTER TABLE " + tableIdentifier() +
                " MODIFY COLUMN player_name VARCHAR(255) NOT NULL DEFAULT ''";
        try (Statement modifyStatement = connection.createStatement()) {
            modifyStatement.executeUpdate(modifySql);
        }

        String updateSql = "UPDATE " + tableIdentifier() +
                " SET player_name = '' WHERE player_name IS NULL";
        try (Statement updateStatement = connection.createStatement()) {
            updateStatement.executeUpdate(updateSql);
        }
        connection.commit();
    }

    private String sanitizedIdentifier(String identifier) {
        return identifier.replace("`", "");
    }
}
