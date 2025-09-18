# PlayerStats Placeholder Instructions

PlayerStats ships with a PlaceholderAPI expansion that lets you expose the plugin's lookups directly in any plugin that supports PlaceholderAPI.

---

## General syntax

```
%playerstats_<target>|<statistic>[|<sub-statistic>][|option=value][|flag]...%
```

* `<target>` – The type of lookup that should be executed. See the dedicated sections below for the supported targets.
* `<statistic>` – Any vanilla `org.bukkit.Statistic` name. Use uppercase and underscores (`MINE_BLOCK`, `JUMP`, `DAMAGE_TAKEN`, ...).
* `<sub-statistic>` – Required for block, item and entity statistics. Use the corresponding `Material` or `EntityType` enum name (for example `diamond_ore`, `stone`, `zombie`).
* Additional segments after the statistic are treated as options (`key=value`) or flags (single tokens). Segments are case-insensitive unless stated otherwise.
* Segments can be separated with `|`. The parser also understands the legacy underscore (`_`) and colon/comma formats, but the pipe format is the recommended and most reliable syntax.

### Common options and flags

| Option / Flag      | Applies to      | Description |
|--------------------|-----------------|-------------|
| `player=<name>`    | `player`, `top` | Overrides the looked-up player. When omitted for player placeholders, the player that triggered the placeholder is used. |
| `world=<world>`    | all             | Restricts the lookup to a specific world. Requires statistics to be synced per-world. |
| `size=<number>`    | `top`           | Sets the desired size of the generated top list. Values ≤ 0 fall back to the configured maximum. |
| `position=<number>` or `pos=<number>` | `top` | Returns data for the exact 1-based position. |
| `top=<number>`     | `top`           | Alias for `size=<number>`. |
| `rank=<number>`    | `top`           | Alias for `position=<number>` when used as an option. |
| `allowexcluded=<true|false>` | `player` | Controls whether excluded players may be looked up. Defaults to the value configured in `config.yml`. |
| `formatted` / `format` | all | Returns the formatted message similar to the `/statistic` command instead of just the raw value. |
| `value` / `raw`    | all             | Returns only the numerical value (no formatting, no player name). For top placeholders, the value of the selected entry is returned. |
| `rank` (flag)      | `top`           | When combined with `player=<name>` it returns the ranking position instead of the value. |

> **Tip:** The first unknown segment after the statistic that is not a recognized option or flag is treated as a sub-statistic. You can therefore omit `sub-statistic=` and supply `diamond_ore` or `zombie` directly after the statistic.

---

## Player placeholders

*Syntax:* `%playerstats_player|<statistic>[|<sub-statistic>][|options/flags]%`

Player placeholders look up a statistic for a single player. When `player=<name>` is omitted, the player that triggered the placeholder is used (requires player context; console executions must specify a player).

**Examples**

* `%playerstats_player|JUMP|player=Notch%` – Raw total jumps for Notch.
* `%playerstats_player|MINE_BLOCK|diamond_ore|formatted%` – Formatted diamond ore mining count for the player that triggered the placeholder.
* `%playerstats_steve|WALK_ONE_CM|world=world_nether%` – Alias form that infers the `player` target from the first segment.

---

## Server placeholders

*Syntax:* `%playerstats_server|<statistic>[|<sub-statistic>][|options/flags]%`

Server placeholders return the combined total for all players on the server (optionally restricted to a single world).

**Examples**

* `%playerstats_server|MOB_KILLS%` – Total mob kills across the entire server.
* `%playerstats_server|MINE_BLOCK|ancient_debris|formatted%` – Formatted server total for ancient debris mined.

---

## Top placeholders

*Syntax:* `%playerstats_top|<statistic>[|<sub-statistic>][|options/flags]%`

Top placeholders generate and expose top lists for a statistic.

### Returning the full formatted list

* `%playerstats_top|PLAY_ONE_MINUTE|size=5|formatted%` – Returns the full formatted top 5 list.

### Returning a specific position

* `%playerstats_top|KILL_ENTITY|zombie|position=1%` – Player name at rank 1 for zombie kills.
* `%playerstats_top|KILL_ENTITY|zombie|position=1|value%` – Raw value (number of zombie kills) at rank 1.
* `%playerstats_top|KILL_ENTITY|zombie|position=1|formatted%` – Formatted entry for rank 1 (`1. Player - value`).

### Looking up a specific player

* `%playerstats_top|MINE_BLOCK|diamond_ore|player=Notch%` – Name of the entry matching Notch (useful when aliases are used).
* `%playerstats_top|MINE_BLOCK|diamond_ore|player=Notch|value%` – Diamond ore mined by Notch in the top list.
* `%playerstats_top|MINE_BLOCK|diamond_ore|player=Notch|rank%` – Rank of Notch in the top list.
* `%playerstats_top|MINE_BLOCK|diamond_ore|player=Notch|formatted%` – Formatted output for Notch's entry (rank, name, value).

When neither `position` nor `player` is specified, the placeholder returns information about the first entry:

* Without flags: the leading player's name.
* With `value`: the leading player's raw value.
* With `formatted`: the formatted first entry (`1. Player - value`).

If the generated top list has no entries (for example because the statistic has never been recorded), the placeholder returns an empty string.

---

## Legacy formats

For backwards compatibility, the expansion can still parse legacy placeholder syntaxes:

* `%playerstats_<target>_<statistic>_<sub-statistic?>_<options...>%` – Underscore-delimited form. Options such as `player=`, `size=`, `position=` and flags like `formatted` still work.
* `%playerstats:<target>,<statistic>,<options...>%` – Colon/comma form. When using `%playerstats_top:<position>,<statistic>,...%` the position is automatically converted to `position=<number>`.

Use the pipe-separated format for all new placeholders to avoid ambiguities.

---

## Error handling

* Unknown targets or statistics produce empty output and log a message to the server console.
* Player placeholders require either a player context or an explicit `player=<name>`. They respect the plugin's exclusion settings unless `allowexcluded=true` is passed.
* Invalid positions or players that are not present in the generated top list result in an empty string.

---

## Troubleshooting

1. Ensure PlaceholderAPI is installed and the PlayerStats expansion is registered (`/papi ecloud list` or check console on startup/reload).
2. Confirm that the statistic and sub-statistic names match Bukkit enum names (`Statistic`, `Material`, `EntityType`). The placeholder parser automatically replaces `+` with `_` to support legacy inputs.
3. If you expect per-world results, make sure your server synchronizes statistics per world and pass the `world=<world>` option.
4. Use the `formatted` flag when you want output similar to `/statistic` command results.

