package net.exylia.commons.v2.scoreboard.integration;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

class TabScoreboardHook {

    static void register(net.exylia.commons.v2.scoreboard.core.ScoreboardManager manager) {
        TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, event -> {
            UUID uuid = event.getPlayer().getUniqueId();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;
            manager.getScoreboard(player).ifPresent(instance -> instance.reinitialize());
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
