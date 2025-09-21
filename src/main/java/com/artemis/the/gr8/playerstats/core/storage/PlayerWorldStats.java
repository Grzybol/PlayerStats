package com.artemis.the.gr8.playerstats.core.storage;

import com.google.gson.annotations.SerializedName;
import org.bukkit.Statistic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Struktura danych do przechowywania statystyk per world dla gracza.
 */
public class PlayerWorldStats {
    // Mapowanie: nazwa świata -> (statystyka -> wartość)
    private final Map<String, Map<Statistic, Integer>> worldStats = new HashMap<>();

    @SerializedName("playerName")
    private String playerName = "";

    public void setStat(String world, Statistic stat, int value) {
        worldStats.computeIfAbsent(world, k -> new HashMap<>()).put(stat, value);
    }

    public int getStat(String world, Statistic stat) {
        return worldStats.getOrDefault(world, new HashMap<>()).getOrDefault(stat, 0);
    }

    public Map<String, Map<Statistic, Integer>> getAllStats() {
        return worldStats;
    }

    public String getPlayerName() {
        return playerName != null ? playerName : "";
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "";
    }

    public boolean clearWorld(String world) {
        return worldStats.remove(world) != null;
    }

    public boolean hasWorld(String world) {
        return worldStats.containsKey(world);
    }

    public Set<String> getRecordedWorlds() {
        return new HashSet<>(worldStats.keySet());
    }
}
