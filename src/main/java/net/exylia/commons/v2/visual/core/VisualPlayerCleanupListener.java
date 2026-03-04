package net.exylia.commons.v2.visual.core;

import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.renderer.BossBarRenderer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VisualPlayerCleanupListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        cleanup(event.getPlayer());
    }

    private void cleanup(Player player) {
        VisualManager manager = VisualManager.getInstance();
        if (!manager.isInitialized()) {
            return;
        }

        manager.cancelAll(player.getUniqueId());
        BossBarRenderer.getInstance().removeAllBossBars(player.getUniqueId());
        CacheManager.getInstance().invalidatePlayer(player);
    }
}
