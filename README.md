<p align="center">
   <img src="src/main/resources/images/logo_gray_rounded.png">
</p>


## About
PlayerStats is a Minecraft server plugin that adds a command to view player statistics in 
top-10 format or individually. Currently tested on all versions between **1.16.5** and **1.21.8** on platforms:
- Bukkit
- Spigot
- Paper
- Purpur

(It's possible PlayerStats works on other platforms too, but these are the ones I have explicitly tested.)

&nbsp;

## Features 
* **Easy to use**
  - One central command that can:
    - Explain **how to use** the plugin with `/statistic`
      ![Usage](src/main/resources/images/usage.png)
    - Show you the **top 10** on your server for all possible statistics with `/statistic ... top`
      ![Top_10](src/main/resources/images/top_10.png)
    - See those same statistics for any **individual player** with `/statistic ... player`
      ![Individual_Stat](src/main/resources/images/individual_stat.png)
    - Or look up the **combined total** of everyone on your server
      ![New_Numbers](src/main/resources/images/new_numbers.png)
    - Guide you through the available options while you type with an extensive **tab-complete** feature
      ![Tab_Complete](src/main/resources/images/tab_complete.png)
    - See the output in a **readable format** that makes sense in the Minecraft world, with more information in hover-text:
      ![Damage_Format](src/main/resources/images/damage_format.png)
    - **Share statistics** that you look up with the other players in chat:
      ![Shared_Top_10](src/main/resources/images/shared_top_10.png)


* **No set-up required**
   - PlayerStats will work correctly regardless of how long your server has already existed - it doesn't 
     have to be present when you start a new world
   - Data is retrieved directly from already existing playerfiles, so you don't have to 
     set up a database, use scoreboards, or anything of the sort


* **PlaceholderAPI support**
   - PlayerStats now ships with a built-in PlaceholderAPI expansion. Install PlaceholderAPI, reload, and the placeholders are registered automatically.
   - Use placeholders with the syntax `%playerstats_<target>|<statistic>[|<sub-statistic>][|option=value][|flag]%` where `<target>` can be `player`, `server`, or `top`.
   - Examples:
     - `%playerstats_player|JUMP|player=Notch%` – total jumps for Notch.
     - `%playerstats_server|MOB_KILLS|formatted%` – formatted server-wide mob kills.
     - `%playerstats_top|MINE_BLOCK|diamond_ore|position=1|formatted%` – formatted top entry for diamond ore mining.


* **Safe**
   - PlayerStats uses **multi-threading** to ensure server performance does not suffer and 
     players cannot crash the server by spamming its commands
   - This also means that calculating statistics will be very **fast**    


* **Customizable**  
    - You can customize the following (and more):
      - Choose which **range of units** you want to display your time-, damage- and distance-based statistics in:
        ![Time_Format](src/main/resources/images/time_format.png)
      - **Automatically translate** statistics to the language of the client that views them, or customize the statistic-names through the **language.yml** file
      - Use festive formatting, or enable rainbow mode whenever!
        ![Translated](src/main/resources/images/translated.png)   
      - Only show statistics for **whitelisted** players
      - Exclude statistics from **banned** players
      - Exclude statistics from specific players with `/statexclude`
      - Limit who is allowed to **share statistics** in chat and how often players can share
      - Limit statistics based on when a player **last joined**.  
        This option can be particularly useful if you have had a lot of players join your server in the past
        whose statistics aren't of particular interest to your current player-base.
        On top of that, limiting the amount of players shown in the top 10 can greatly increase performance speed.
      - The **colors** you want the output to be
      - You can go for default Minecraft chat colors, or use **hex colors**!
      - Whether you want the output to have additional **style**, such as italics 
    - You can configure the following **permissions**:
      - `playerstats.stat` for using the general command (true for everyone by default)
      - `playerstats.share` for sharing statistics in chat (true for everyone by default)
      - `playerstats.reload` for reloading the config (only for OP players by default)
      - `playerstats.exclude` to exclude players from top- and server-statistics (only for OP players by default)

&nbsp;

## Storage configuration
PlayerStats keeps the per-world statistic cache in a dedicated storage backend. By default the plugin
uses a JSON file located inside the plugin data folder, but you can switch to MariaDB for centralized
deployments.

```yaml
storage:
  type: file # or mariadb
  file:
    path: world_stats.json
  mariadb:
    host: localhost
    port: 3306
    database: playerstats
    username: playerstats
    password: change-me
    table: playerstats_world_stats
    use-ssl: true
```

### Using the file backend
Nothing else is required – the file is created automatically on first start inside the plugin data
folder. Existing installations are migrated transparently to the new location.

### Using MariaDB
1. Create a database and user with privileges to create tables and modify data.
2. Update the `storage.mariadb` section with your connection information.
3. Reload the plugin with `/statisticreload` or restart the server. The plugin will create the
   required table (`playerstats_world_stats` by default) and migrate any existing JSON data to the
   database.

Switching the backend later is supported; running `/statisticreload` applies the new configuration
and automatically reinitializes the connection.

&nbsp;

## API Usage
To import the PlayerStats API with Maven, add the following dependency and repository to your POM.xnl:

```xml
<repositories>
    <repository>
        <id>sonatype-oss-snapshots1</id> <!-- the OSSRH repository for snapshots -->
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
     </repository>
</repositories>
 
<dependencies>
    <dependency>
        <groupId>io.github.ithotl</groupId>
        <artifactId>PlayerStats</artifactId>
        <version>2.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
You can download the sources and Javadocs through your code editor, or visit the Javadocs [here](https://s01.oss.sonatype.org/service/local/repositories/snapshots/archive/io/github/ithotl/PlayerStats/2.0-SNAPSHOT/PlayerStats-2.0-20230228.110241-1-javadoc.jar/!/com/artemis/the/gr8/playerstats/api/PlayerStats.html).  
To get an instance of the API, you can do the following:

```java
PlayerStats playerStats = PlayerStats.getAPI();
```

&nbsp;

## Author Info
I am a relatively new programmer, and this is one of my first projects. I greatly enjoyed making it, 
and I tried to make it as efficient as I could. If you have any questions, remarks, or suggestions, 
please let me know! You can find me [here](https://github.com/Artemis-the-gr8) on GitHub. 

&nbsp;

## Licence
PlayerStats is licenced under the MIT licence. Please see [LICENCE](LICENSE) for more information.
