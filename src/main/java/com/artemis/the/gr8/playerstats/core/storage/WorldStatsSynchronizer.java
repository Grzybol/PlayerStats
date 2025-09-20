package com.artemis.the.gr8.playerstats.core.storage;

import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.Closable;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger.LogLevel;
import com.artemis.the.gr8.playerstats.core.utils.Reloadable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinates persistence of {@link WorldStatsDatabase} using the configured backend.
 */
public class WorldStatsSynchronizer implements Reloadable, Closable {

    private final JavaPlugin plugin;
    private final ConfigHandler config;
    private final WorldStatsDatabase database;

    private WorldStatsPersistence persistence;
    private StorageType currentType;

    public WorldStatsSynchronizer(JavaPlugin plugin, ConfigHandler config, WorldStatsDatabase database) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
    }

    public synchronized void initialize() throws Exception {
        applyStorageSettings(true);
        PluginLogger.log(LogLevel.INFO, "World statistics storage initialized using " + describeType(currentType));
    }

    public synchronized WorldStatsDatabase getDatabase() {
        return database;
    }

    public synchronized StorageType getCurrentType() {
        return currentType;
    }

    public synchronized void save() throws Exception {
        if (persistence != null) {
            persistence.save(database);
        }
    }

    @Override
    public synchronized void reload() {
        try {
            applyStorageSettings(false);
            PluginLogger.log(LogLevel.INFO, "World statistics storage reloaded using " + describeType(currentType));
        } catch (Exception ex) {
            PluginLogger.logException(ex, "WorldStatsSynchronizer", "reload");
            PluginLogger.log(LogLevel.WARNING, "Failed to reload world statistics storage. Previous backend remains active.");
        }
    }

    @Override
    public synchronized void close() {
        if (persistence == null) {
            return;
        }
        try {
            persistence.save(database);
        } catch (Exception ex) {
            PluginLogger.logException(ex, "WorldStatsSynchronizer", "close-save");
        }
        try {
            persistence.close();
        } catch (Exception ex) {
            PluginLogger.logException(ex, "WorldStatsSynchronizer", "close");
        } finally {
            persistence = null;
            currentType = null;
        }
    }

    private void applyStorageSettings(boolean initialSetup) throws Exception {
        StorageType newType = config.getStorageType();
        WorldStatsPersistence newPersistence = createPersistence(newType);
        WorldStatsPersistence previousPersistence = persistence;
        StorageType previousType = currentType;
        Map<UUID, PlayerWorldStats> snapshot = new HashMap<>(database.getAll());

        if (previousPersistence != null) {
            try {
                previousPersistence.save(database);
            } catch (Exception ex) {
                PluginLogger.logException(ex, "WorldStatsSynchronizer", "persist-before-switch");
            }
        }

        database.getAll().clear();

        try {
            newPersistence.initialize();
            newPersistence.load(database);

            if (database.getAll().isEmpty() && !snapshot.isEmpty()) {
                database.getAll().putAll(snapshot);
                newPersistence.save(database);
                PluginLogger.log(LogLevel.INFO, "Migrated in-memory world statistics to " + describeType(newType) + ".");
            } else if (database.getAll().isEmpty() && newType == StorageType.MARIADB) {
                if (migrateFromFileIfAvailable(newPersistence)) {
                    PluginLogger.log(LogLevel.INFO, "Imported legacy file storage into MariaDB backend.");
                }
            }

            if (previousPersistence != null) {
                try {
                    previousPersistence.close();
                } catch (Exception ex) {
                    PluginLogger.logException(ex, "WorldStatsSynchronizer", "close-previous");
                }
            }

            persistence = newPersistence;
            currentType = newType;
        } catch (Exception ex) {
            database.getAll().clear();
            database.getAll().putAll(snapshot);
            if (previousPersistence != null) {
                persistence = previousPersistence;
                currentType = previousType;
            } else {
                persistence = null;
                currentType = null;
            }
            try {
                newPersistence.close();
            } catch (Exception closeEx) {
                PluginLogger.logException(closeEx, "WorldStatsSynchronizer", "revert-close");
            }
            throw ex;
        }

        if (!initialSetup && newType != previousType) {
            PluginLogger.log(LogLevel.INFO, "Switched world statistics backend from " + describeType(previousType) + " to " + describeType(newType) + ".");
        }
    }

    private WorldStatsPersistence createPersistence(StorageType type) {
        return switch (type) {
            case FILE -> new FileWorldStatsStorage(new File(plugin.getDataFolder(), config.getStorageFilePath()));
            case MARIADB -> new MariaDbWorldStatsStorage(config.getMariaDbSettings());
        };
    }

    private boolean migrateFromFileIfAvailable(WorldStatsPersistence targetPersistence) {
        File legacyFile = new File(plugin.getDataFolder(), config.getStorageFilePath());
        if (!legacyFile.exists() || legacyFile.length() == 0) {
            return false;
        }

        FileWorldStatsStorage legacyStorage = new FileWorldStatsStorage(legacyFile);
        try {
            legacyStorage.initialize();
            legacyStorage.load(database);
            if (!database.getAll().isEmpty()) {
                targetPersistence.save(database);
                return true;
            }
        } catch (Exception ex) {
            PluginLogger.logException(ex, "WorldStatsSynchronizer", "migrateFromFile");
        }
        database.getAll().clear();
        return false;
    }

    private String describeType(StorageType type) {
        if (type == null) {
            return "no storage";
        }
        return type.name().toLowerCase(Locale.ROOT);
    }
}
