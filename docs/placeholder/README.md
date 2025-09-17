# PlayerStats PlaceholderAPI Reference

PlayerStats ships with a built-in [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) expansion that is
registered automatically whenever PlaceholderAPI is installed. This document lists every supported placeholder and
explains how to combine the available options.

## Getting Started

1. Install the PlaceholderAPI plugin on your server.
2. Reload or restart the server. PlayerStats will detect PlaceholderAPI and register the `playerstats` expansion.
3. Use the placeholders documented below in scoreboards, chat formats, holograms, or any other PlaceholderAPI-aware
   plugin.

## General Syntax

```
%playerstats_<target>|<statistic>[|<sub-statistic>][|option=value][|flag]...%
```

* `<target>` decides which kind of lookup is performed:
  * `player` / `p` / `me`
  * `server` / `s`
  * `top` / `t`
* `<statistic>` must be the Bukkit statistic key (for example `MINE_BLOCK`, `PLAY_ONE_MINUTE`, `MOB_KILLS`). Use the
  `/statistic` command in-game or check the [Spigot statistic list](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html)
  to discover the available names.
* `<sub-statistic>` is required for block, item, and entity statistics. Provide the Bukkit material or entity name, such
  as `diamond_ore`, `diamond_pickaxe`, or `creeper`. Leave it empty for untyped statistics.
* Additional segments after the statistic are interpreted as key/value options or simple flags (see below). Segments are
  separated with the pipe character (`|`). Whitespace is ignored.

When PlayerStats cannot parse a placeholder (for example, due to an unknown statistic or invalid option) an empty string
is returned.

## Common Options and Flags

| Option / Flag          | Applies to targets | Description |
|------------------------|--------------------|-------------|
| `player=<name>`        | player, top        | Player to look up. Defaults to the player triggering the placeholder when the target is `player`. |
| `world=<world>`        | all                | Restrict the lookup to a specific world. This requires statistics to be synchronised per-world. |
| `size=<number>` / `top=<number>` | top | Amount of players to include in the top list. Defaults to the plugin configuration value. |
| `position=<number>` / `pos=<number>` / `rank=<number>` | top | Return the entry at the given 1-based rank. |
| `formatted` / `format` | all                | Output the fully formatted message (identical to the `/statistic` command). |
| `value` / `raw`        | all                | Output just the numerical value. For `top` targets this applies to the selected entry. |
| `rank` (flag)          | top                | When used together with `player=<name>`, return the player’s ranking instead of the value. |
| `allowexcluded=<true|false>` | player, top | Override the plugin setting that blocks lookups for excluded players. |

Options are case-insensitive. If the same option is supplied multiple times, the last value wins.

## Target Details

### Player Placeholders

Structure: `%playerstats_player|<statistic>[|<sub-statistic>][|options]%`

* Defaults to the player who triggered the placeholder when no `player=` option is provided.
* Respects the plugin’s configuration for excluded players. Use `allowexcluded=true` if you explicitly want to query an
  excluded player.
* Examples:
  * `%playerstats_player|JUMP%` – total jumps for the triggering player.
  * `%playerstats_player|MINE_BLOCK|diamond_ore|player=Notch%` – diamond ore mined by Notch.
  * `%playerstats_player|MOB_KILLS|world=world_nether|formatted%` – formatted mob kills for the Nether world.

### Server Placeholders

Structure: `%playerstats_server|<statistic>[|<sub-statistic>][|options]%`

* Returns the combined total of all players. Add `formatted` to receive the decorated chat output, or `value` to get just
  the raw number.
* Supports `world=` to limit the lookup to a specific world.
* Examples:
  * `%playerstats_server|MINE_BLOCK|diamond_ore%` – total diamond ore mined on the server.
  * `%playerstats_server|PLAY_TIME|world=world_the_end|formatted%` – formatted playtime accumulated in the End.

### Top Placeholders

Structure: `%playerstats_top|<statistic>[|<sub-statistic>][|options]%`

* Without extra options the first entry of the top list is returned. Use `formatted` to display the entire top list.
* `player=<name>` finds that player in the ranking and returns either their value or, with the `rank` flag, their position.
* `position=<n>` selects the nth entry in the top list. Combine with `value` or `formatted` to change the output.
* Examples:
  * `%playerstats_top|MINE_BLOCK|diamond_ore|formatted%` – formatted top list for diamond ore mining.
  * `%playerstats_top|JUMP|player=Notch|rank%` – Notch’s rank in the jump leaderboard.
  * `%playerstats_top|MOB_KILLS|position=3|value%` – raw mob kill count for the third place.
  * `%playerstats_top|FISH_CAUGHT|size=25|position=5%` – name of the player currently in 5th place in a top-25 list.

## Tips

* All statistic, material, entity, player, and world names are case-insensitive.
* PlaceholderAPI’s built-in tools such as `/papi parse` are extremely helpful for testing placeholders before adding them
  to configuration files.
* Remember to reload or restart any plugins that cache placeholder results after you change placeholder configurations.
