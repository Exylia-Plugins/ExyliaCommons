package net.exylia.commons.v2.scoreboard.integration;

import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bridge to the TAB plugin. TAB wipes/claims the client scoreboard when it
 * loads a player (shortly after join) and periodically re-sends its own
 * state, which silently erases the packet-only sidebar sent by this module.
 * This integration re-sends our board after TAB loads a player and hands the
 * slot back to TAB when we hide ours.
 */
public final class TabIntegration {

    private static final String LOG_PREFIX = "[Scoreboard] [TabIntegration] ";

    private static Boolean tabEnabled;

    private TabIntegration() {
    }

    public static boolean isTabEnabled() {
        if (tabEnabled == null) {
            tabEnabled = Bukkit.getPluginManager().isPluginEnabled("TAB");
            Bukkit.getLogger().info(LOG_PREFIX + (tabEnabled
                    ? "TAB plugin detected -> scoreboard handoff enabled"
                    : "TAB plugin not present -> integration disabled"));
        }
        return tabEnabled;
    }

    public static void register(ScoreboardManager manager) {
        if (!isTabEnabled()) return;
        try {
            TabScoreboardHook.register(manager);
            Bukkit.getLogger().info(LOG_PREFIX + "registered TAB PlayerLoadEvent hook -> packet scoreboards will be re-sent after TAB loads a player");
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "failed to register TAB hook: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    public static void resetScoreboard(Player player) {
        if (!isTabEnabled()) return;
        try {
            TabScoreboardHook.resetScoreboard(player);
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "resetScoreboard(" + player.getName() + ") failed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }
}
