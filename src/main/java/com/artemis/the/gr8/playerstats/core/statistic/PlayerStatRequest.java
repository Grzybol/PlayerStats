package com.artemis.the.gr8.playerstats.core.statistic;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public final class PlayerStatRequest extends StatRequest<Integer> implements RequestGenerator<Integer> {


    // Nowy konstruktor: globalnie
    public PlayerStatRequest(String playerName) {
        this(Bukkit.getConsoleSender(), playerName, null);
    }

    // Nowy konstruktor: per world
    public PlayerStatRequest(String playerName, String worldName) {
        this(Bukkit.getConsoleSender(), playerName, worldName);
    }

    // Nowy konstruktor: z senderem i worldName
    public PlayerStatRequest(CommandSender sender, String playerName, String worldName) {
        super(sender);
        super.configureForPlayer(playerName);
        if (worldName != null) {
            super.getSettings().setWorldName(worldName);
        }
    }

    // Zachowujemy stary konstruktor dla kompatybilności
    public PlayerStatRequest(CommandSender sender, String playerName) {
        this(sender, playerName, null);
    }

    @Override
    public boolean isValid() {
        if (!hasValidTarget()) {
            return false;
        }
        return super.hasMatchingSubStat();
    }

    private boolean hasValidTarget() {
        StatRequest.Settings settings = super.getSettings();
        if (settings.getPlayerName() == null) {
            return false;
        }

        OfflinePlayerHandler offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        if (offlinePlayerHandler.isExcludedPlayer(settings.getPlayerName())) {
            return ConfigHandler.getInstance().allowPlayerLookupsForExcludedPlayers();
        } else {
            return offlinePlayerHandler.isIncludedPlayer(settings.getPlayerName());
        }
    }

    @Override
    public StatRequest<Integer> untyped(@NotNull Statistic statistic) {
        super.configureUntyped(statistic);
        return this;
    }

    @Override
    public StatRequest<Integer> blockOrItemType(@NotNull Statistic statistic, @NotNull Material material) {
        super.configureBlockOrItemType(statistic, material);
        return this;
    }

    @Override
    public StatRequest<Integer> entityType(@NotNull Statistic statistic, @NotNull EntityType entityType) {
        super.configureEntityType(statistic, entityType);
        return this;
    }
}