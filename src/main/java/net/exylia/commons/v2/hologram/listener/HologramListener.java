package net.exylia.commons.v2.hologram.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class HologramListener implements Listener {
    private final HologramManager manager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        manager.getVisibilityManager().trackPlayer(player);

        manager.getAllHolograms().stream()
                .filter(Hologram::isPerPlayer)
                .filter(h -> h.canSee(player))
                .forEach(hologram -> hologram.showTo(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        manager.getAllHolograms().stream()
                .filter(Hologram::isPerPlayer)
                .forEach(hologram -> hologram.cleanupPlayer(player.getUniqueId()));

        manager.getVisibilityManager().untrackPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        manager.getVisibilityManager().updatePlayerLocation(player);

        manager.getAllHolograms().stream()
                .filter(Hologram::isPerPlayer)
                .forEach(hologram -> {
                    boolean canSeeNow = hologram.canSee(player);
                    boolean isSeeing = hologram.getPlayerDisplays().containsKey(player.getUniqueId());

                    if (canSeeNow && !isSeeing) {
                        hologram.showTo(player);
                    } else if (!canSeeNow && isSeeing) {
                        hologram.hideFrom(player);
                    }
                });
    }
}
