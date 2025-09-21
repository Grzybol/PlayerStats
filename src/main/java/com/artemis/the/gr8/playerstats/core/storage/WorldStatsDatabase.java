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
        return getOrCreatePlayerStats(uuid, null);
    }

    public PlayerWorldStats getOrCreatePlayerStats(UUID uuid, String playerName) {
        PlayerWorldStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerWorldStats());
        if (playerName != null && !playerName.isEmpty()) {
            stats.setPlayerName(playerName);
        }
        return stats;
    }

    public void setStat(UUID uuid, String world, Statistic stat, int value) {
        getOrCreatePlayerStats(uuid).setStat(world, stat, value);
    }

    public int getStat(UUID uuid, String world, Statistic stat) {
        return getOrCreatePlayerStats(uuid).getStat(world, stat);
    }

    public void setPlayerName(UUID uuid, String playerName) {
        PlayerWorldStats stats = getOrCreatePlayerStats(uuid);
        String incomingName = playerName != null ? playerName : "";
        if (!incomingName.isEmpty() || stats.getPlayerName().isEmpty()) {
            stats.setPlayerName(incomingName);
        }
    }

    public String getPlayerName(UUID uuid) {
        return getOrCreatePlayerStats(uuid).getPlayerName();
    }

    public void mergePlayerStats(UUID uuid, PlayerWorldStats stats) {
        if (stats == null) {
            return;
        }

        PlayerWorldStats existing = playerStats.computeIfAbsent(uuid, k -> new PlayerWorldStats());
        String incomingName = stats.getPlayerName();
        if (!incomingName.isEmpty() || existing.getPlayerName().isEmpty()) {
            existing.setPlayerName(incomingName);
        }

        for (Map.Entry<String, Map<Statistic, Integer>> entry : stats.getAllStats().entrySet()) {
            Map<Statistic, Integer> targetWorldStats = existing.getAllStats()
                    .computeIfAbsent(entry.getKey(), key -> new HashMap<>());
            Map<Statistic, Integer> incomingWorldStats = entry.getValue();
            if (targetWorldStats == incomingWorldStats) {
                continue;
            }
            targetWorldStats.clear();
            targetWorldStats.putAll(incomingWorldStats);
        }
    }

    public void replaceAll(Map<UUID, PlayerWorldStats> newData) {
        playerStats.clear();
        if (newData == null) {
            return;
        }
        for (Map.Entry<UUID, PlayerWorldStats> entry : newData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerWorldStats stats = entry.getValue();
            if (uuid != null && stats != null) {
                mergePlayerStats(uuid, stats);
            }
        }
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
