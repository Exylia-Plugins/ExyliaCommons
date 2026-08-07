package net.exylia.commons.v2.scoreboard.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class ScoreboardListener implements Listener {

    private static final String LOG_PREFIX = "[Scoreboard] [ScoreboardListener] ";

    private final ScoreboardManager manager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getLogger().info(LOG_PREFIX + "onQuit(" + event.getPlayer().getName() + ") -> clearing player scoreboard state");
        manager.clearPlayer(event.getPlayer());
    }
}
