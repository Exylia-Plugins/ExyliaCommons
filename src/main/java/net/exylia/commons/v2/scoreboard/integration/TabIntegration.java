package net.exylia.commons.v2.scoreboard.integration;

import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TabIntegration {

    private static Boolean tabEnabled = null;

    public static boolean isTabEnabled() {
        if (tabEnabled == null) {
            tabEnabled = Bukkit.getPluginManager().isPluginEnabled("TAB");
        }
        return tabEnabled;
    }

    public static void register(ScoreboardManager manager) {
        if (!isTabEnabled()) return;
        TabScoreboardHook.register(manager);
    }

    public static void resetScoreboard(Player player) {
        if (!isTabEnabled()) return;
        TabScoreboardHook.resetScoreboard(player);
    }
}
