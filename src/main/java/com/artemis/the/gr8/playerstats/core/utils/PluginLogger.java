package com.artemis.the.gr8.playerstats.core.utils;

import com.artemis.the.gr8.playerstats.core.enums.DebugLevel;
import org.betterbox.elasticBuffer.ElasticBufferAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerStats logger that writes to a dedicated log file, forwards important
 * information to the console and optionally mirrors entries to ElasticBuffer.
 */
public final class PluginLogger {

    private static PluginLogger instance;

    public enum LogLevel {
        INFO, WARNING, ERROR, DEBUG, KILL_EVENT, COMMAND, PLACEHOLDER, BLOCK_BREAK, BLOCK_PLACE, PLAYER_INTERACT
    }

    private final JavaPlugin plugin;
    private final File logFile;
    private Set<LogLevel> enabledLogLevels;
    private DebugLevel debugLevel;
    private final ConcurrentHashMap<String, Integer> threadNames;

    public boolean isElasticBufferEnabled;
    public ElasticBufferAPI api;

    private PluginLogger(@NotNull JavaPlugin plugin, @NotNull Set<LogLevel> enabledLogLevels) {
        this.plugin = plugin;
        this.enabledLogLevels = enabledLogLevels.isEmpty() ? EnumSet.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR)
                : EnumSet.copyOf(enabledLogLevels);
        this.debugLevel = DebugLevel.LOW;
        this.threadNames = new ConcurrentHashMap<>();

        File logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists() && !logFolder.mkdirs()) {
            plugin.getLogger().warning("PluginLogger: Could not create log directory at " + logFolder.getAbsolutePath());
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        String fileName = formatter.format(date) + ".log";
        logFile = new File(logFolder, fileName);

        try {
            if (!logFile.exists() && !logFile.createNewFile()) {
                plugin.getLogger().severe("PluginLogger: Could not create log file!");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("PluginLogger: Could not create log file! " + e.getMessage());
        }
    }

    public static synchronized void init(@NotNull JavaPlugin plugin) {
        init(plugin, EnumSet.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR));
    }

    public static synchronized void init(@NotNull JavaPlugin plugin, @NotNull Set<LogLevel> enabledLogLevels) {

        instance = new PluginLogger(plugin, enabledLogLevels);
    }

    public static @NotNull PluginLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PluginLogger has not been initialized yet");
        }
        return instance;
    }

    public static void setDebugLevel(int level) {
        getInstance().applyDebugLevel(level);
    }

    public static void setEnabledLogLevels(@NotNull Set<LogLevel> configEnabledLogLevels) {
        getInstance().updateEnabledLogLevels(configEnabledLogLevels);
    }

    public static void setEnabledEventItems(@NotNull Set<LogLevel> configEnabledEventItems) {
        setEnabledLogLevels(configEnabledEventItems);
    }

    public static void enableElasticBuffer(@NotNull ElasticBufferAPI api) {
        PluginLogger logger = getInstance();
        logger.api = api;
        logger.isElasticBufferEnabled = true;
        logger.log(LogLevel.INFO, "ElasticBuffer logging enabled for PlayerStats");
    }

    public static void disableElasticBuffer() {
        PluginLogger logger = getInstance();
        logger.api = null;
        logger.isElasticBufferEnabled = false;
        logger.log(LogLevel.WARNING, "ElasticBuffer logging disabled for PlayerStats");
    }

    public static void log(@NotNull LogLevel level, @NotNull String message) {
        getInstance().logInternal(level, message, null);
    }

    public static void log(@NotNull LogLevel level, @NotNull String message, @Nullable String transactionId) {
        getInstance().logInternal(level, message, transactionId);
    }

    public static void logLowLevelMsg(@NotNull String content) {
        log(LogLevel.INFO, content);
    }

    public static void logLowLevelTask(@NotNull String taskName, long startTime) {
        printTime(taskName, startTime);
    }

    public static void logMediumLevelMsg(@NotNull String content) {
        PluginLogger logger = getInstance();
        if (logger.debugLevel != DebugLevel.LOW) {
            log(LogLevel.DEBUG, content);
        }
    }

    public static void logMediumLevelTask(@NotNull String taskName, long startTime) {
        PluginLogger logger = getInstance();
        if (logger.debugLevel != DebugLevel.LOW) {
            printTime(taskName, startTime);
        }
    }

    public static void logHighLevelMsg(@NotNull String content) {
        if (getInstance().debugLevel == DebugLevel.HIGH) {
            log(LogLevel.DEBUG, content);
        }
    }

    public static void logWarning(@NotNull String content) {
        log(LogLevel.WARNING, content);
    }

    public static void logException(@NotNull Exception exception, String caughtBy, @Nullable String additionalInfo) {
        PluginLogger logger = getInstance();
        String extraInfo = (additionalInfo != null) ? " [" + additionalInfo + "]" : "";
        String info = " (" + caughtBy + extraInfo + ")";

        log(LogLevel.ERROR, exception + info);
        if (logger.debugLevel == DebugLevel.HIGH) {
            exception.printStackTrace();
        }
    }

    public static void actionCreated(int taskLength) {
        PluginLogger logger = getInstance();
        if (logger.debugLevel != DebugLevel.LOW) {
            logger.threadNames.clear();
            log(LogLevel.DEBUG, "Initial Action created for " + taskLength + " Players. Processing...");
        }
    }

    public static void subActionCreated(@NotNull String threadName) {
        PluginLogger logger = getInstance();
        if (logger.debugLevel == DebugLevel.HIGH) {
            logger.threadNames.putIfAbsent(threadName, logger.threadNames.size());
        }
    }

    public static void actionRunning(@NotNull String threadName) {
        PluginLogger logger = getInstance();
        if (logger.debugLevel != DebugLevel.LOW) {
            logger.threadNames.putIfAbsent(threadName, logger.threadNames.size());
        }
    }

    public static void actionFinished() {
        PluginLogger logger = getInstance();
        if (logger.debugLevel != DebugLevel.LOW) {
            log(LogLevel.DEBUG, "Finished Recursive Action! In total " +
                    logger.threadNames.size() + " Threads were used");
        }
        if (logger.debugLevel == DebugLevel.HIGH) {
            log(LogLevel.DEBUG, Collections.list(logger.threadNames.keys()).toString());
        }
    }

    private static void printTime(@NotNull String taskName, long startTime) {
        log(LogLevel.DEBUG, taskName + " (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    private void applyDebugLevel(int level) {
        if (level == 2) {
            debugLevel = DebugLevel.MEDIUM;
            enabledLogLevels = EnumSet.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR, LogLevel.DEBUG, LogLevel.PLACEHOLDER);
        } else if (level >= 3) {
            debugLevel = DebugLevel.HIGH;
            enabledLogLevels = EnumSet.allOf(LogLevel.class);
        } else {
            debugLevel = DebugLevel.LOW;
            enabledLogLevels = EnumSet.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR);
        }
        log(LogLevel.INFO, "Debug level updated to " + debugLevel + ", enabled log levels "
                + Arrays.toString(enabledLogLevels.toArray()));
    }

    private void updateEnabledLogLevels(@NotNull Set<LogLevel> configEnabledLogLevels) {
        enabledLogLevels = configEnabledLogLevels.isEmpty()
                ? EnumSet.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR)
                : EnumSet.copyOf(configEnabledLogLevels);
        log(LogLevel.INFO, "Enabled Log levels " + Arrays.toString(enabledLogLevels.toArray()));
    }

    private void logInternal(@NotNull LogLevel level, @NotNull String message, @Nullable String transactionId) {
        if (!enabledLogLevels.contains(level)) {
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String logMessage = timestamp + " [" + level + "] - " + message;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("PluginLogger: log: Could not write to log file! " + e.getMessage());
        }

        logToConsole(level, message);

        if (isElasticBufferEnabled && api != null) {
            try {
                api.log(message, level.toString(), plugin.getDescription().getName(), transactionId);
            } catch (Exception e) {
                plugin.getLogger().severe("PluginLogger: log: Could not write to ElasticBuffer! " + e.getMessage());
            }
        }
    }

    private void logToConsole(@NotNull LogLevel level, @NotNull String message) {
        switch (level) {
            case ERROR -> plugin.getLogger().severe(message);
            case WARNING -> plugin.getLogger().warning(message);
            default -> plugin.getLogger().info(message);
        }
    }
}

