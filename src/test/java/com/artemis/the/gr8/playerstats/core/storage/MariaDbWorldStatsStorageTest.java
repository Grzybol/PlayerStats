package com.artemis.the.gr8.playerstats.core.storage;

import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import org.bukkit.Statistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MariaDbWorldStatsStorageTest {

    private WorldStatsDatabase database;
    private MariaDbWorldStatsStorage storage;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        database = new WorldStatsDatabase();
        storage = new MariaDbWorldStatsStorage(new ConfigHandler.MariaDbSettings(
                "localhost", 3306, "playerstats", "playerstats",
                "change-me", "playerstats_world_stats", true));
        playerUuid = UUID.randomUUID();
    }

    @Test
    void loadDoesNotOverwriteKnownPlayerNameWithEmptyValue() {
        database.setPlayerName(playerUuid, "KnownPlayer");

        storage.applyLoadedStat(database, playerUuid, "", "world", Statistic.DEATHS, 5);

        assertEquals("KnownPlayer", database.getPlayerName(playerUuid));
    }

    @Test
    void loadAppliesNewNameWhenUnknown() {
        storage.applyLoadedStat(database, playerUuid, "FreshName", "world", Statistic.JUMP, 3);

        assertEquals("FreshName", database.getPlayerName(playerUuid));
    }

    @Test
    void loadResolvesEmptyNameWhenResolverFindsValue() {
        MariaDbWorldStatsStorage resolvingStorage = new TestMariaDbWorldStatsStorage("RecoveredName");

        resolvingStorage.applyLoadedStat(database, playerUuid, "", "world", Statistic.WALK_ONE_CM, 42);

        assertEquals("RecoveredName", database.getPlayerName(playerUuid));
    }

    @Test
    void cacheResolvedPlayerNameUpdatesStats() {
        MariaDbWorldStatsStorage resolvingStorage = new TestMariaDbWorldStatsStorage("ResolvedOnSave");
        PlayerWorldStats stats = database.getOrCreatePlayerStats(playerUuid);

        String resolved = resolvingStorage.cacheResolvedPlayerName(database, playerUuid, stats, "testing");

        assertEquals("ResolvedOnSave", resolved);
        assertEquals("ResolvedOnSave", stats.getPlayerName());
        assertEquals("ResolvedOnSave", database.getPlayerName(playerUuid));
    }

    private static class TestMariaDbWorldStatsStorage extends MariaDbWorldStatsStorage {

        private final String nameToReturn;

        private TestMariaDbWorldStatsStorage(String nameToReturn) {
            super(new ConfigHandler.MariaDbSettings(
                    "localhost", 3306, "playerstats", "playerstats",
                    "change-me", "playerstats_world_stats", true));
            this.nameToReturn = nameToReturn;
        }

        @Override
        protected String resolvePlayerName(UUID uuid) {
            return nameToReturn;
        }
    }
}
