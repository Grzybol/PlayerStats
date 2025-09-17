package com.artemis.the.gr8.playerstats.core.statistic;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public final class ServerStatRequest extends StatRequest<Long> implements RequestGenerator<Long> {


    // Nowy konstruktor: globalnie
    public ServerStatRequest() {
        this(Bukkit.getConsoleSender(), null);
    }

    // Nowy konstruktor: per world
    public ServerStatRequest(String worldName) {
        this(Bukkit.getConsoleSender(), worldName);
    }

    // Nowy konstruktor: z senderem i worldName
    public ServerStatRequest(CommandSender sender, String worldName) {
        super(sender);
        super.configureForServer();
        if (worldName != null) {
            super.getSettings().setWorldName(worldName);
        }
    }

    // Zachowujemy stary konstruktor dla kompatybilności
    public ServerStatRequest(CommandSender sender) {
        this(sender, null);
    }

    @Override
    public boolean isValid() {
        return super.hasMatchingSubStat();
    }

    @Override
    public StatRequest<Long> untyped(@NotNull Statistic statistic) {
        super.configureUntyped(statistic);
        return this;
    }

    @Override
    public StatRequest<Long> blockOrItemType(@NotNull Statistic statistic, @NotNull Material material) {
        super.configureBlockOrItemType(statistic, material);
        return this;
    }

    @Override
    public StatRequest<Long> entityType(@NotNull Statistic statistic, @NotNull EntityType entityType) {
        super.configureEntityType(statistic, entityType);
        return this;
    }
}