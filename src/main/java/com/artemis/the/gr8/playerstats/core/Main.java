package com.artemis.the.gr8.playerstats.core;

import com.artemis.the.gr8.playerstats.api.PlayerStats;
import com.artemis.the.gr8.playerstats.api.StatManager;
import com.artemis.the.gr8.playerstats.api.StatNumberFormatter;
import com.artemis.the.gr8.playerstats.api.StatTextFormatter;
import com.artemis.the.gr8.playerstats.core.commands.*;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.listeners.JoinListener;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.LanguageKeyHandler;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.NumberFormatter;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import com.artemis.the.gr8.playerstats.core.placeholder.PlayerStatsPlaceholderExpansion;
import com.artemis.the.gr8.playerstats.core.sharing.ShareManager;
import com.artemis.the.gr8.playerstats.core.statistic.StatRequestManager;
import com.artemis.the.gr8.playerstats.core.storage.WorldStatsDatabase;
import com.artemis.the.gr8.playerstats.core.storage.WorldStatsSynchronizer;
import com.artemis.the.gr8.playerstats.core.utils.*;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger.LogLevel;
import org.betterbox.elasticBuffer.ElasticBuffer;
import org.betterbox.elasticBuffer.ElasticBufferAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin implements PlayerStats {

    // Lokalna baza statystyk per world
    public static WorldStatsDatabase worldStatsDb;
    private static WorldStatsSynchronizer worldStatsSync;
    private static File worldStatsFile;

    private static JavaPlugin pluginInstance;
    private static PlayerStats playerStatsAPI;
    private static ConfigHandler config;

    private static ThreadManager threadManager;
    private static StatRequestManager statManager;

    private static List<Reloadable> reloadables;
    private static List<Closable> closables;

    private PlayerStatsPlaceholderExpansion placeholderExpansion;
    private boolean placeholderApiActive;

    @Override
    public void onEnable() {
        // Upewnij się, że folder pluginu istnieje
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        PluginLogger.init(this);
        loadElasticBuffer();

        // Inicjalizacja lokalnej bazy statystyk per world
        worldStatsFile = new File(getDataFolder(), "world_stats.json");
        worldStatsDb = new WorldStatsDatabase();
        worldStatsSync = new WorldStatsSynchronizer(worldStatsDb, worldStatsFile);

        // Jeśli plik nie istnieje – utwórz pusty
        if (!worldStatsFile.exists()) {
            try {
                worldStatsFile.createNewFile();
                PluginLogger.log(LogLevel.INFO, "Utworzono pusty world_stats.json");
            } catch (Exception e) {
                PluginLogger.log(LogLevel.WARNING, "Nie udało się utworzyć world_stats.json: " + e.getMessage());
            }
        }

        try {
            worldStatsSync.load();
            PluginLogger.log(LogLevel.INFO, "Załadowano world_stats.json");
        } catch (Exception e) {
            PluginLogger.log(LogLevel.WARNING, "Nie udało się załadować world_stats.json: " + e.getMessage());
        }

        reloadables = new ArrayList<>();
        closables = new ArrayList<>();

        initializeMainClassesInOrder();
        registerCommands();
        registerPlaceholderExpansion();
        setupMetrics();

        // rejestrujemy listener
        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);

        PluginLogger.log(LogLevel.INFO, "Enabled PlayerStats!");
    }

    @Override
    public void onDisable() {
        // Zapis bazy przy wyłączaniu pluginu
        try {
            worldStatsSync.save();
            PluginLogger.log(LogLevel.INFO, "Zapisano world_stats.json");
        } catch (Exception e) {
            PluginLogger.log(LogLevel.WARNING, "Nie udało się zapisać world_stats.json: " + e.getMessage());
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        placeholderApiActive = false;

        closables.forEach(Closable::close);
        PluginLogger.log(LogLevel.INFO, "Disabled PlayerStats!");
    }

    public void reloadPlugin() {
        config.reload();
        reloadables.forEach(Reloadable::reload);
    }

    public static void registerReloadable(Reloadable reloadable) {
        reloadables.add(reloadable);
    }

    public static void registerClosable(Closable closable) {
        closables.add(closable);
    }

    public static @NotNull JavaPlugin getPluginInstance() throws IllegalStateException {
        if (pluginInstance == null) {
            throw new IllegalStateException("PlayerStats is not loaded!");
        }
        return pluginInstance;
    }

    public static @NotNull PlayerStats getPlayerStatsAPI() throws IllegalStateException {
        if (playerStatsAPI == null) {
            throw new IllegalStateException("PlayerStats does not seem to be loaded!");
        }
        return playerStatsAPI;
    }

    private void initializeMainClassesInOrder() {
        pluginInstance = this;
        playerStatsAPI = this;
        config = ConfigHandler.getInstance();

        LanguageKeyHandler.getInstance();
        OfflinePlayerHandler.getInstance();
        OutputManager.getInstance();
        ShareManager.getInstance();

        statManager = new StatRequestManager();
        threadManager = new ThreadManager(this);
    }

    private void registerCommands() {
        TabCompleter tabCompleter = new TabCompleter();

        PluginCommand statcmd = this.getCommand("statistic");
        if (statcmd != null) {
            statcmd.setExecutor(new StatCommand(threadManager));
            statcmd.setTabCompleter(tabCompleter);
        }
        PluginCommand excludecmd = this.getCommand("statisticexclude");
        if (excludecmd != null) {
            excludecmd.setExecutor(new ExcludeCommand());
            excludecmd.setTabCompleter(tabCompleter);
        }

        PluginCommand reloadcmd = this.getCommand("statisticreload");
        if (reloadcmd != null) {
            reloadcmd.setExecutor(new ReloadCommand(threadManager));
        }
        PluginCommand sharecmd = this.getCommand("statisticshare");
        if (sharecmd != null) {
            sharecmd.setExecutor(new ShareCommand());
        }
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new PlayerStatsPlaceholderExpansion(this);
            placeholderApiActive = placeholderExpansion.register();
            if (placeholderApiActive) {
                PluginLogger.log(LogLevel.INFO, "Registered PlayerStats placeholders with PlaceholderAPI");
            } else {
                PluginLogger.log(LogLevel.WARNING, "Failed to register PlayerStats placeholders with PlaceholderAPI");
                placeholderExpansion = null;
            }
        } else {
            placeholderApiActive = false;
            placeholderExpansion = null;
        }
    }

    private void loadElasticBuffer() {
        try {
            PluginManager pm = Bukkit.getPluginManager();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                PluginLogger.log(LogLevel.WARNING, "[PlayerStats] Initialization delay interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            Plugin elasticBufferPlugin = pm.getPlugin("ElasticBuffer");
            if (elasticBufferPlugin == null) {
                PluginLogger.log(LogLevel.DEBUG, "ElasticBuffer plugin not detected; skipping integration.");
                return;
            }
            if (!(elasticBufferPlugin instanceof ElasticBuffer elasticBuffer)) {
                PluginLogger.log(LogLevel.WARNING, "Found ElasticBuffer plugin but type mismatch: " + elasticBufferPlugin.getClass().getName());
                return;
            }
            PluginLogger.enableElasticBuffer(new ElasticBufferAPI(elasticBuffer));
        } catch (Exception e) {
            PluginLogger.logException(e, "Main", "loadElasticBuffer");
        }
    }

    private void setupMetrics() {
        final Metrics metrics = new Metrics(pluginInstance, 15923);
        metrics.addCustomChart(new Metrics.SimplePie("using_placeholder_expansion",
                () -> placeholderApiActive ? "yes" : "no"));

        CommandCounter counter = CommandCounter.getInstance();
        metrics.addCustomChart(new Metrics.AdvancedPie("commands_used_the_last_30_minutes",
                counter::getCommandCounts));
    }

    @Override
    public @NotNull String getVersion() {
        return String.valueOf(this.getDescription().getVersion().charAt(0));
    }

    @Override
    public StatManager getStatManager() {
        return statManager;
    }

    @Override
    public StatTextFormatter getStatTextFormatter() {
        return OutputManager.getInstance().getMainMessageBuilder();
    }

    @Contract(" -> new")
    @Override
    public @NotNull StatNumberFormatter getStatNumberFormatter() {
        return new NumberFormatter();
    }
}
