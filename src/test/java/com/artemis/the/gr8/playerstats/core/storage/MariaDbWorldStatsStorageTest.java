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
}
