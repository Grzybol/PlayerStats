package com.artemis.the.gr8.playerstats.core.commands;

import com.artemis.the.gr8.playerstats.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class StatisticsCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    private static final String RESET_SUBCOMMAND = "reset";
    private static final String CONFIRM_ARGUMENT = "confirm";
    private static final long CONFIRMATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    private final Map<String, PendingReset> pendingConfirmations = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerstats.statistics.reset")) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        if (args.length < 2 || !RESET_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            sendUsage(sender, label);
            return true;
        }

        String worldName = args[1];
        String senderKey = getSenderKey(sender);
        removeIfExpired(senderKey);

        if (args.length == 2) {
            if (!Main.hasWorldStatistics(worldName)) {
                sender.sendMessage(ChatColor.YELLOW + "Brak zapisanych statystyk dla świata " + ChatColor.AQUA + worldName + ChatColor.YELLOW + ".");
                return true;
            }

            pendingConfirmations.put(senderKey, new PendingReset(worldName));
            sender.sendMessage(ChatColor.GOLD + "Potwierdź reset wpisując " + ChatColor.RED + "/" + label + " " + RESET_SUBCOMMAND + " " + worldName + " " + CONFIRM_ARGUMENT + ChatColor.GOLD + " w ciągu 2 minut.");
            return true;
        }

        if (args.length >= 3 && CONFIRM_ARGUMENT.equalsIgnoreCase(args[2])) {
            PendingReset pending = pendingConfirmations.get(senderKey);
            if (pending == null || pending.isExpired() || !pending.worldName.equalsIgnoreCase(worldName)) {
                sender.sendMessage(ChatColor.YELLOW + "Brak oczekującej operacji resetu dla świata " + ChatColor.AQUA + worldName + ChatColor.YELLOW + ".");
                pendingConfirmations.remove(senderKey);
                return true;
            }

            pendingConfirmations.remove(senderKey);
            if (Main.resetWorldStatistics(pending.worldName)) {
                sender.sendMessage(ChatColor.GREEN + "Statystyki dla świata " + ChatColor.AQUA + pending.worldName + ChatColor.GREEN + " zostały zresetowane.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Nie znaleziono statystyk do resetu dla świata " + ChatColor.AQUA + pending.worldName + ChatColor.YELLOW + ".");
            }
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private void sendUsage(@NotNull CommandSender sender, @NotNull String label) {
        sender.sendMessage(ChatColor.YELLOW + "Użycie: " + ChatColor.AQUA + "/" + label + " " + RESET_SUBCOMMAND + " <world> [" + CONFIRM_ARGUMENT + "]");
    }

    private void removeIfExpired(String senderKey) {
        PendingReset pending = pendingConfirmations.get(senderKey);
        if (pending != null && pending.isExpired()) {
            pendingConfirmations.remove(senderKey);
        }
    }

    private String getSenderKey(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return sender.getName();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("playerstats.statistics.reset")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of(RESET_SUBCOMMAND), new ArrayList<>());
        }

        if (args.length == 2 && RESET_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            return StringUtil.copyPartialMatches(args[1], getKnownWorlds(), new ArrayList<>());
        }

        if (args.length == 3 && RESET_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            return StringUtil.copyPartialMatches(args[2], List.of(CONFIRM_ARGUMENT), new ArrayList<>());
        }

        return Collections.emptyList();
    }

    private List<String> getKnownWorlds() {
        Set<String> recorded = Main.getWorldsWithStatistics();
        List<String> suggestions = new ArrayList<>(recorded);
        for (World world : Bukkit.getWorlds()) {
            if (!suggestions.contains(world.getName())) {
                suggestions.add(world.getName());
            }
        }
        return suggestions;
    }

    private static final class PendingReset {
        private final String worldName;
        private final long expiresAt;

        private PendingReset(String worldName) {
            this.worldName = worldName;
            this.expiresAt = System.currentTimeMillis() + CONFIRMATION_TIMEOUT;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
