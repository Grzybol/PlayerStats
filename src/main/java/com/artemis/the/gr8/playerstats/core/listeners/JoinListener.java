package com.artemis.the.gr8.playerstats.core.listeners;

import com.artemis.the.gr8.playerstats.core.Main;
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

import java.util.UUID;

/**
 * Obsługuje joiny i aktualizację statystyk per-world.
 */
@ApiStatus.Internal
public class JoinListener implements Listener {

    private final OfflinePlayerHandler offlinePlayerHandler;
    private final WorldStatsDatabase db;

    public JoinListener() {
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        db = Main.worldStatsDb; // zakładam, że masz to w Main jako singleton
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();
        if (!player.hasPlayedBefore() && !offlinePlayerHandler.isExcludedPlayer(player.getUniqueId())) {
            offlinePlayerHandler.addNewIncludedPlayer(player);
        }

        // snapshot wszystkich statów na start
        syncPlayerStats(player);
    }

    @EventHandler
    public void onStatisticIncrease(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String world = player.getWorld().getName();
        Statistic stat = event.getStatistic();

        if (stat.getType() == Statistic.Type.UNTYPED) {
            int newValue = player.getStatistic(stat); // globalna wartość
            db.setStat(uuid, world, stat, newValue);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // przy zmianie świata zrób snapshot dla nowego świata
        Player player = event.getPlayer();
        syncPlayerStats(player);
    }

    private void syncPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        String world = player.getWorld().getName();

        for (Statistic stat : Statistic.values()) {
            if (stat.getType() == Statistic.Type.UNTYPED) {
                int value = player.getStatistic(stat);
                db.setStat(uuid, world, stat, value);
            }
        }
    }
}
