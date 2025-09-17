package com.artemis.the.gr8.playerstats.core.storage;

import org.bukkit.Statistic;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Struktura danych do przechowywania statystyk per world dla gracza.
 */
public class PlayerWorldStats {
    // Mapowanie: nazwa świata -> (statystyka -> wartość)
    private final Map<String, Map<Statistic, Integer>> worldStats = new HashMap<>();

    public void setStat(String world, Statistic stat, int value) {
        worldStats.computeIfAbsent(world, k -> new HashMap<>()).put(stat, value);
    }

    public int getStat(String world, Statistic stat) {
        return worldStats.getOrDefault(world, new HashMap<>()).getOrDefault(stat, 0);
    }

    public Map<String, Map<Statistic, Integer>> getAllStats() {
        return worldStats;
    }
}
