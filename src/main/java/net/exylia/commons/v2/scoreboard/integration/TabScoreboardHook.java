package net.exylia.commons.v2.scoreboard.integration;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Only class that touches TAB API classes directly; must only be loaded when
 * the TAB plugin is present (guarded by {@link TabIntegration}).
 */
class TabScoreboardHook {

    private TabScoreboardHook() {
    }

    static void register(net.exylia.commons.v2.scoreboard.core.ScoreboardManager manager) {
        TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, event -> {
            UUID uuid = event.getPlayer().getUniqueId();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;
            // TAB's event bus may fire off the main thread; scheduleReinit
            // defers the packet re-send to a main-thread task with a small
            // delay so TAB finishes writing its own state first.
            manager.getScoreboard(player).ifPresent(instance -> manager.scheduleReinit(player, instance));
        });
    }

    static void resetScoreboard(Player player) {
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer == null) return;
        ScoreboardManager scoreboardManager = TabAPI.getInstance().getScoreboardManager();
        if (scoreboardManager == null) return;
        scoreboardManager.resetScoreboard(tabPlayer);
    }
}
