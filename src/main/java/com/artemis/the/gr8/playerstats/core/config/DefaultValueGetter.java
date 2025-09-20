package com.artemis.the.gr8.playerstats.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class DefaultValueGetter {

    private final FileConfiguration config;
    private final Map<String, Object> defaultValuesToAdjust;

    public DefaultValueGetter(FileConfiguration configuration) {
        config = configuration;
        defaultValuesToAdjust = new HashMap<>();
    }

    public Map<String, Object> getValuesToAdjust() {
        checkTopListDefault();
        checkDefaultColors();
        checkStorageSettings();
        return defaultValuesToAdjust;
    }

    private void checkTopListDefault() {
        String oldTitle = config.getString("top-list-title");
        if (oldTitle != null && oldTitle.equalsIgnoreCase("Top [x]")) {
            defaultValuesToAdjust.put("top-list-title", "Top");
        }
    }

    /**
     * Adjusts some of the default colors to migrate from versions 2
     * or 3 to version 4 and above.
     */
    private void checkDefaultColors() {
        addValueIfNeeded("top-list.title", "yellow", "#FFD52B");
        addValueIfNeeded("top-list.title", "#FFEA40", "#FFD52B");
        addValueIfNeeded("top-list.stat-names", "yellow", "#FFD52B");
        addValueIfNeeded("top-list.stat-names", "#FFEA40", "#FFD52B");
        addValueIfNeeded("top-list.sub-stat-names", "#FFD52B", "yellow");

        addValueIfNeeded("individual-statistics.stat-names", "yellow", "#FFD52B");
        addValueIfNeeded("individual-statistics.sub-stat-names", "#FFD52B", "yellow");
        addValueIfNeeded("total-server.title", "gold", "#55AAFF");
        addValueIfNeeded("total-server.server-name", "gold", "#55AAFF");
        addValueIfNeeded("total-server.stat-names", "yellow", "#FFD52B");
        addValueIfNeeded("total-server.sub-stat-names", "#FFD52B", "yellow");
    }

    private void addValueIfNeeded(String path, String oldValue, String newValue) {
        String configString = config.getString(path);
        if (configString != null && configString.equalsIgnoreCase(oldValue)) {
            defaultValuesToAdjust.put(path, newValue);
        }
    }

    private void addValueIfMissing(String path, Object value) {
        if (!config.contains(path)) {
            defaultValuesToAdjust.put(path, value);
        }
    }

    private void checkStorageSettings() {
        addValueIfMissing("storage.type", "file");
        addValueIfMissing("storage.file.path", "world_stats.json");

        ConfigurationSection mariaDbSection = config.getConfigurationSection("storage.mariadb");
        if (mariaDbSection == null) {
            defaultValuesToAdjust.put("storage.mariadb.host", "localhost");
            defaultValuesToAdjust.put("storage.mariadb.port", 3306);
            defaultValuesToAdjust.put("storage.mariadb.database", "playerstats");
            defaultValuesToAdjust.put("storage.mariadb.username", "playerstats");
            defaultValuesToAdjust.put("storage.mariadb.password", "change-me");
            defaultValuesToAdjust.put("storage.mariadb.table", "playerstats_world_stats");
            defaultValuesToAdjust.put("storage.mariadb.use-ssl", true);
        } else {
            addValueIfMissing("storage.mariadb.host", "localhost");
            addValueIfMissing("storage.mariadb.port", 3306);
            addValueIfMissing("storage.mariadb.database", "playerstats");
            addValueIfMissing("storage.mariadb.username", "playerstats");
            addValueIfMissing("storage.mariadb.password", "change-me");
            addValueIfMissing("storage.mariadb.table", "playerstats_world_stats");
            addValueIfMissing("storage.mariadb.use-ssl", true);
        }
    }
}