package net.exylia.commons.v2.scoreboard.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@RequiredArgsConstructor
public class ScoreboardListener implements Listener {

    private final ScoreboardManager manager;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        manager.clearPlayerScoreboards(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        manager.getScoreboard(player).ifPresent(instance ->
                Tasks.later(() -> {
                    if (player.isOnline()) instance.reinitialize();
                }, 1L)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        manager.getScoreboard(player).ifPresent(instance ->
                Tasks.later(() -> {
                    if (player.isOnline()) instance.reinitialize();
                }, 1L)
        );
    }
}
