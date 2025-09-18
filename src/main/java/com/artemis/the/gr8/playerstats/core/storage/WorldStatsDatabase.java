package com.artemis.the.gr8.playerstats.core.storage;

import org.bukkit.Statistic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Główna klasa do przechowywania statystyk wszystkich graczy per world.
 * Mapowanie: UUID gracza -> PlayerWorldStats
 */
public class WorldStatsDatabase {
    private final Map<UUID, PlayerWorldStats> playerStats = new HashMap<>();

    public PlayerWorldStats getOrCreatePlayerStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerWorldStats());
    }

    public void setStat(UUID uuid, String world, Statistic stat, int value) {
        getOrCreatePlayerStats(uuid).setStat(world, stat, value);
    }

    public int getStat(UUID uuid, String world, Statistic stat) {
        return getOrCreatePlayerStats(uuid).getStat(world, stat);
    }

    public Map<UUID, PlayerWorldStats> getAll() {
        return playerStats;
    }

    public boolean resetWorld(String world) {
        boolean removedAny = false;
        for (PlayerWorldStats stats : playerStats.values()) {
            removedAny |= stats.clearWorld(world);
        }
        return removedAny;
    }

    public boolean hasWorld(String world) {
        for (PlayerWorldStats stats : playerStats.values()) {
            if (stats.hasWorld(world)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getRecordedWorlds() {
        Set<String> worlds = new HashSet<>();
        for (PlayerWorldStats stats : playerStats.values()) {
            worlds.addAll(stats.getRecordedWorlds());
        }
        return worlds;
    }
}
