package net.exylia.commons.v2.scoreboard.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class PlayerCleanupListener implements Listener {

    private final ScoreboardManager manager;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        manager.clearPlayerScoreboards(player);
    }
}
