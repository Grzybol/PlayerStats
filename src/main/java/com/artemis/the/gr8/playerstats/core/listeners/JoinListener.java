package com.artemis.the.gr8.playerstats.core.listeners;

import com.artemis.the.gr8.playerstats.core.storage.WorldStatsDatabase;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Obsługuje joiny i aktualizację statystyk per-world.
 */
@ApiStatus.Internal
public class JoinListener implements Listener {

    private final OfflinePlayerHandler offlinePlayerHandler;
    private final WorldStatsDatabase db;

    private final Map<UUID, Map<Statistic, Integer>> trackedStatistics;

    public JoinListener(WorldStatsDatabase database) {
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        db = database;
        trackedStatistics = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();
        if (!player.hasPlayedBefore() && !offlinePlayerHandler.isExcludedPlayer(player.getUniqueId())) {
            offlinePlayerHandler.addNewIncludedPlayer(player);
        }

        // zaktualizuj lokalny cache statystyk na start
        syncPlayerStats(player);
    }

    @EventHandler
    public void onStatisticIncrease(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String world = player.getWorld().getName();
        Statistic stat = event.getStatistic();

        if (stat.getType() != Statistic.Type.UNTYPED) {
            return;
        }

        int delta = event.getNewValue() - event.getPreviousValue();
        if (delta <= 0) {
            delta = deriveDeltaFromCache(uuid, stat, event.getNewValue());
        }
        if (delta <= 0) {
            updateTrackedValue(uuid, stat, event.getNewValue());
            return;
        }

        if (!db.hasWorld(world)) {
            // świat został zresetowany – zacznij zbierać dane od nowa
            resetTrackedStats(uuid);
        }

        int currentValue = db.getStat(uuid, world, stat);
        db.setStat(uuid, world, stat, currentValue + delta);
        updateTrackedValue(uuid, stat, event.getNewValue());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // przy zmianie świata zresetuj cache i przygotuj nowe wartości referencyjne
        Player player = event.getPlayer();
        syncPlayerStats(player);
    }

    private void syncPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        String world = player.getWorld().getName();

        if (!db.hasWorld(world)) {
            // świeżo zresetowany świat – nie zapisuj nic do bazy, jedynie zresetuj cache
            resetTrackedStats(uuid);
            return;
        }

        Map<Statistic, Integer> snapshot = trackedStatistics.computeIfAbsent(uuid, key -> new EnumMap<>(Statistic.class));
        snapshot.clear();
        for (Statistic stat : Statistic.values()) {
            if (stat.getType() == Statistic.Type.UNTYPED) {
                snapshot.put(stat, player.getStatistic(stat));
            }
        }
    }

    private void updateTrackedValue(UUID uuid, Statistic stat, int newValue) {
        Map<Statistic, Integer> playerStats = trackedStatistics.computeIfAbsent(uuid, key -> new EnumMap<>(Statistic.class));
        playerStats.put(stat, newValue);
    }

    private int deriveDeltaFromCache(UUID uuid, Statistic stat, int newValue) {
        Map<Statistic, Integer> playerStats = trackedStatistics.get(uuid);
        if (playerStats == null) {
            return 0;
        }

        Integer previousValue = playerStats.get(stat);
        if (previousValue == null) {
            return 0;
        }

        return newValue - previousValue;
    }

    private void resetTrackedStats(UUID uuid) {
        trackedStatistics.remove(uuid);
    }
}
