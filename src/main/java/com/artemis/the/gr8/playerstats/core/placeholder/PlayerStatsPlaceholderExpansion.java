package com.artemis.the.gr8.playerstats.core.placeholder;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.StatResult;
import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.statistic.PlayerStatRequest;
import com.artemis.the.gr8.playerstats.core.statistic.ServerStatRequest;
import com.artemis.the.gr8.playerstats.core.statistic.StatRequestManager;
import com.artemis.the.gr8.playerstats.core.statistic.TopStatRequest;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import com.artemis.the.gr8.playerstats.core.utils.PluginLogger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PlaceholderAPI expansion that exposes PlayerStats lookups directly from the plugin.
 * <p>
 * The supported placeholder format is structured as follows:
 * <pre>
 *   %playerstats_{target}|{statistic}[|{sub-statistic}][|option=value][|flag]...%
 * </pre>
 * Target can be {@code player}, {@code server} or {@code top}. Options control the
 * lookup and output. Supported options and flags:
 * <ul>
 *   <li>{@code player=<name>} – overrides the player that is looked up. When omitted
 *   for player lookups, the player that triggers the placeholder is used.</li>
 *   <li>{@code world=<world>} – limits the lookup to the specified world (requires
 *   statistics to be synced per-world).</li>
 *   <li>{@code size=<number>} – size of the top list (only for {@code top}).</li>
 *   <li>{@code position=<number>} – 1-based position to return from the top list.</li>
 *   <li>{@code formatted} – returns the formatted message (similar to /stat output).</li>
 *   <li>{@code value} – return only the numerical value (for {@code top} placeholders,
 *   this applies to the selected entry).</li>
 *   <li>{@code rank} – when used together with {@code player=<name>} on {@code top}
 *   placeholders, returns the ranking position instead of the value.</li>
 * </ul>
 */
public final class PlayerStatsPlaceholderExpansion extends PlaceholderExpansion {

    private static final Pattern OPTION_SPLIT_PATTERN = Pattern.compile("\\|");

    private final Main plugin;
    private final EnumHandler enumHandler;
    private final ConfigHandler config;
    private final OfflinePlayerHandler offlinePlayerHandler;

    public PlayerStatsPlaceholderExpansion(@NotNull Main plugin) {
        this.plugin = plugin;
        this.enumHandler = EnumHandler.getInstance();
        this.config = ConfigHandler.getInstance();
        this.offlinePlayerHandler = OfflinePlayerHandler.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playerstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return Objects.requireNonNullElse(plugin.getDescription().getVersion(), plugin.getDescription().getName());
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Received placeholder request '" + params + "' from " + (player != null ? player.getName() : "null"));
        try {
            PlaceholderArguments arguments = PlaceholderArguments.parse(params, player, enumHandler, offlinePlayerHandler);
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Parsed placeholder arguments: " + arguments.debugSummary());
            return switch (arguments.target) {
                case PLAYER -> handlePlayerPlaceholder(arguments);
                case SERVER -> handleServerPlaceholder(arguments);
                case TOP -> handleTopPlaceholder(arguments);
            };
        } catch (IllegalArgumentException exception) {
            PluginLogger.logException(exception, "PlayerStatsPlaceholderExpansion",
                    "onPlaceholderRequest params='" + params + "'");
            return "";
        }
    }

    private @Nullable String handlePlayerPlaceholder(@NotNull PlaceholderArguments arguments) {
        if (arguments.playerName == null) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Player placeholder missing player context: " + arguments.debugSummary());
            return "";
        }

        if (!arguments.allowExcluded && offlinePlayerHandler.isExcludedPlayer(arguments.playerName)) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Player placeholder blocked for excluded player '" + arguments.playerName + "'");
            return "";
        }

        PlayerStatRequest request = arguments.worldName == null
                ? new PlayerStatRequest(arguments.playerName)
                : new PlayerStatRequest(arguments.playerName, arguments.worldName);

        StatRequest<Integer> configuredRequest = configureRequest(request, arguments.statistic, arguments.subStatisticName);
        if (configuredRequest == null || !configuredRequest.isValid()) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Invalid player placeholder request: " + arguments.debugSummary());
            return "";
        }

        StatResult<Integer> result = StatRequestManager.execute(configuredRequest);
        if (arguments.formatted) {
            String output = result.formattedString();
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Player placeholder formatted output: '" + output + "'");
            return output;
        }
        String value = String.valueOf(result.value());
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Player placeholder numeric output: '" + value + "'");
        return value;
    }

    private @Nullable String handleServerPlaceholder(@NotNull PlaceholderArguments arguments) {
        ServerStatRequest request = arguments.worldName == null
                ? new ServerStatRequest()
                : new ServerStatRequest(arguments.worldName);

        StatRequest<Long> configuredRequest = configureRequest(request, arguments.statistic, arguments.subStatisticName);
        if (configuredRequest == null || !configuredRequest.isValid()) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Invalid server placeholder request: " + arguments.debugSummary());
            return "";
        }

        StatResult<Long> result = StatRequestManager.execute(configuredRequest);
        if (arguments.formatted) {
            String output = result.formattedString();
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Server placeholder formatted output: '" + output + "'");
            return output;
        }
        String value = String.valueOf(result.value());
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Server placeholder numeric output: '" + value + "'");
        return value;
    }

    private @Nullable String handleTopPlaceholder(@NotNull PlaceholderArguments arguments) {
        int topSize = arguments.topListSize == null || arguments.topListSize <= 0
                ? config.getTopListMaxSize()
                : arguments.topListSize;

        TopStatRequest request = arguments.worldName == null
                ? new TopStatRequest(topSize)
                : new TopStatRequest(topSize, arguments.worldName);

        StatRequest<LinkedHashMap<String, Integer>> configuredRequest = configureRequest(request, arguments.statistic, arguments.subStatisticName);
        if (configuredRequest == null || !configuredRequest.isValid()) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Invalid top placeholder request: " + arguments.debugSummary());
            return "";
        }

        StatResult<LinkedHashMap<String, Integer>> result = StatRequestManager.execute(configuredRequest);
        LinkedHashMap<String, Integer> values = result.value();
        if (values.isEmpty()) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Top placeholder returned no results: " + arguments.debugSummary());
            return "";
        }

        if (arguments.formatted && arguments.position == null && arguments.playerName == null) {
            String output = result.formattedString();
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Top placeholder formatted list output: '" + output + "'");
            return output;
        }

        if (arguments.playerName != null) {
            return handleTopPlayer(arguments, values);
        }

        if (arguments.position != null) {
            return handleTopPosition(arguments, values);
        }

        Map.Entry<String, Integer> firstEntry = values.entrySet().iterator().next();
        if (arguments.returnValue) {
            String value = String.valueOf(firstEntry.getValue());
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Top placeholder returning value for first entry: '" + value + "'");
            return value;
        }
        if (arguments.formatted) {
            String output = formatTopEntry(1, firstEntry.getKey(), firstEntry.getValue());
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Top placeholder formatted first entry: '" + output + "'");
            return output;
        }
        String playerName = firstEntry.getKey();
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Top placeholder returning first entry player: '" + playerName + "'");
        return playerName;
    }

    private @Nullable String handleTopPlayer(@NotNull PlaceholderArguments arguments, LinkedHashMap<String, Integer> values) {
        int index = 1;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(arguments.playerName)) {
                if (arguments.returnRank) {
                    String rank = String.valueOf(index);
                    PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                            "Top placeholder returning rank '" + rank + "' for player '" + entry.getKey() + "'");
                    return rank;
                }
                if (arguments.returnValue) {
                    String value = String.valueOf(entry.getValue());
                    PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                            "Top placeholder returning value '" + value + "' for player '" + entry.getKey() + "'");
                    return value;
                }
                if (arguments.formatted) {
                    String output = formatTopEntry(index, entry.getKey(), entry.getValue());
                    PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                            "Top placeholder formatted output for player '" + entry.getKey() + "': '" + output + "'");
                    return output;
                }
                PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                        "Top placeholder returning player name '" + entry.getKey() + "' at position " + index);
                return entry.getKey();
            }
            index++;
        }
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Top placeholder could not find player '" + arguments.playerName + "'");
        return "";
    }

    private @Nullable String handleTopPosition(@NotNull PlaceholderArguments arguments, LinkedHashMap<String, Integer> values) {
        if (arguments.position <= 0 || arguments.position > values.size()) {
            PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                    "Top placeholder requested invalid position " + arguments.position + ": " + arguments.debugSummary());
            return "";
        }

        Iterator<Map.Entry<String, Integer>> iterator = values.entrySet().iterator();
        int counter = 1;
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (counter == arguments.position) {
                if (arguments.returnValue) {
                    String value = String.valueOf(entry.getValue());
                    PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                            "Top placeholder returning value '" + value + "' for position " + counter);
                    return value;
                }
                if (arguments.formatted) {
                    String output = formatTopEntry(counter, entry.getKey(), entry.getValue());
                    PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                            "Top placeholder formatted output for position " + counter + ": '" + output + "'");
                    return output;
                }
                PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                        "Top placeholder returning player '" + entry.getKey() + "' for position " + counter);
                return entry.getKey();
            }
            counter++;
        }
        PluginLogger.log(PluginLogger.LogLevel.PLACEHOLDER,
                "Top placeholder failed to resolve position " + arguments.position + ": " + arguments.debugSummary());
        return "";
    }

    private String formatTopEntry(int position, String playerName, int value) {
        return position + ". " + playerName + " - " + value;
    }

    private <T> @Nullable StatRequest<T> configureRequest(@NotNull RequestGenerator<T> request,
                                                          @NotNull Statistic statistic,
                                                          @Nullable String subStatisticName) {
        try {
            return switch (statistic.getType()) {
                case UNTYPED -> request.untyped(statistic);
                case BLOCK -> {
                    Material block = enumHandler.getBlockEnum(subStatisticName);
                    yield block != null ? request.blockOrItemType(statistic, block) : null;
                }
                case ITEM -> {
                    Material item = enumHandler.getItemEnum(subStatisticName);
                    yield item != null ? request.blockOrItemType(statistic, item) : null;
                }
                case ENTITY -> {
                    EntityType entity = enumHandler.getEntityEnum(subStatisticName);
                    yield entity != null ? request.entityType(statistic, entity) : null;
                }
            };
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private enum PlaceholderTarget {
        PLAYER, SERVER, TOP
    }

    private record PlaceholderArguments(PlaceholderTarget target,
                                        Statistic statistic,
                                        @Nullable String subStatisticName,
                                        @Nullable String playerName,
                                        @Nullable String worldName,
                                        @Nullable Integer topListSize,
                                        @Nullable Integer position,
                                        boolean formatted,
                                        boolean returnValue,
                                        boolean returnRank,
                                        boolean allowExcluded) {

        private static final Set<String> KNOWN_FLAGS = Set.of("formatted", "format", "value", "raw", "rank");

        private static PlaceholderArguments parse(String params,
                                                   @Nullable Player player,
                                                   @NotNull EnumHandler enumHandler,
                                                   @NotNull OfflinePlayerHandler offlinePlayerHandler) {
            String[] segments = OPTION_SPLIT_PATTERN.split(params);
            if (segments.length < 2) {
                throw new IllegalArgumentException("Placeholder needs at least a target and statistic");
            }

            PlaceholderTarget target = parseTarget(segments[0]);
            Statistic statistic = parseStatistic(segments[1], enumHandler);

            String subStat = null;
            String requestedPlayer = null;
            String worldName = null;
            Integer topSize = null;
            Integer position = null;
            boolean formatted = false;
            boolean returnValue = false;
            boolean returnRank = false;
            boolean allowExcluded = ConfigHandler.getInstance().allowPlayerLookupsForExcludedPlayers();

            for (int i = 2; i < segments.length; i++) {
                String segment = segments[i].trim();
                if (segment.isEmpty()) {
                    continue;
                }

                int equalsIndex = segment.indexOf('=');
                if (equalsIndex > 0) {
                    String key = segment.substring(0, equalsIndex).toLowerCase(Locale.ENGLISH);
                    String value = segment.substring(equalsIndex + 1).trim();
                    switch (key) {
                        case "player" -> requestedPlayer = value;
                        case "world" -> worldName = value;
                        case "size", "top" -> topSize = tryParseInt(value);
                        case "position", "pos", "rank" -> position = tryParseInt(value);
                        case "allowexcluded" -> allowExcluded = Boolean.parseBoolean(value);
                        default -> {
                            if (subStat == null && !KNOWN_FLAGS.contains(segment.toLowerCase(Locale.ENGLISH))) {
                                subStat = value;
                            }
                        }
                    }
                    continue;
                }

                String lowerSegment = segment.toLowerCase(Locale.ENGLISH);
                if (lowerSegment.equals("formatted") || lowerSegment.equals("format")) {
                    formatted = true;
                    continue;
                }
                if (lowerSegment.equals("value") || lowerSegment.equals("raw")) {
                    returnValue = true;
                    continue;
                }
                if (lowerSegment.equals("rank")) {
                    returnRank = true;
                    continue;
                }

                if (subStat == null) {
                    subStat = segment;
                }
            }

            if (target == PlaceholderTarget.PLAYER && requestedPlayer == null) {
                requestedPlayer = player != null ? player.getName() : null;
            }

            if (target == PlaceholderTarget.PLAYER && requestedPlayer != null &&
                    !offlinePlayerHandler.isIncludedPlayer(requestedPlayer) && !allowExcluded) {
                requestedPlayer = null;
            }

            return new PlaceholderArguments(target, statistic, subStat, requestedPlayer,
                    worldName, topSize, position, formatted, returnValue, returnRank, allowExcluded);
        }

        public String debugSummary() {
            return "target=" + target +
                    ", statistic=" + statistic +
                    (subStatisticName != null ? ":" + subStatisticName : "") +
                    ", player=" + playerName +
                    ", world=" + worldName +
                    ", topSize=" + topListSize +
                    ", position=" + position +
                    ", formatted=" + formatted +
                    ", valueOnly=" + returnValue +
                    ", rank=" + returnRank +
                    ", allowExcluded=" + allowExcluded;
        }

        private static PlaceholderTarget parseTarget(String segment) {
            return switch (segment.toLowerCase(Locale.ENGLISH)) {
                case "player", "p", "me" -> PlaceholderTarget.PLAYER;
                case "server", "s" -> PlaceholderTarget.SERVER;
                case "top", "t" -> PlaceholderTarget.TOP;
                default -> throw new IllegalArgumentException("Unknown target: " + segment);
            };
        }

        private static Statistic parseStatistic(String segment, EnumHandler enumHandler) {
            Statistic statistic = enumHandler.getStatEnum(segment.trim());
            if (statistic == null) {
                throw new IllegalArgumentException("Unknown statistic: " + segment);
            }
            return statistic;
        }

        private static Integer tryParseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
