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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class HologramListener implements Listener {
    private static final int MOVE_THRESHOLD_BLOCKS = 3;
    private static final double MOVE_THRESHOLD_SQUARED = MOVE_THRESHOLD_BLOCKS * MOVE_THRESHOLD_BLOCKS;

    private final HologramManager manager;
    private final Map<UUID, Location> lastUpdateLocation = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        manager.getVisibilityManager().trackPlayer(player);

        manager.getAllHolograms().stream()
                .filter(Hologram::isPerPlayer)
                .filter(h -> h.canSee(player))
                .forEach(hologram -> hologram.showTo(player));

        manager.getAllHolograms().stream()
                .filter(h -> !h.isPerPlayer())
                .forEach(hologram -> {
                    if (!hologram.canSee(player)) {
                        for (org.bukkit.entity.TextDisplay display : hologram.getGlobalDisplays()) {
                            player.hideEntity(manager.getPlugin(), display);
                        }
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        manager.getAllHolograms().stream()
                .filter(Hologram::isPerPlayer)
                .forEach(hologram -> hologram.cleanupPlayer(player.getUniqueId()));

        manager.getVisibilityManager().untrackPlayer(player);
        lastUpdateLocation.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location lastLoc = lastUpdateLocation.get(playerId);

        if (lastLoc != null) {
            if (!to.getWorld().equals(lastLoc.getWorld())) {
                lastUpdateLocation.put(playerId, to.clone());
            } else {
                double distanceSquared = to.distanceSquared(lastLoc);
                if (distanceSquared < MOVE_THRESHOLD_SQUARED) {
                    return;
                }
                lastUpdateLocation.put(playerId, to.clone());
            }
        } else {
            lastUpdateLocation.put(playerId, to.clone());
        }

        manager.getVisibilityManager().updatePlayerLocation(player);

        double maxViewDistance = 64.0;
        Set<String> nearbyIds = manager.getVisibilityManager()
                .getSpatialChunkManager()
                .getNearbyHologramIds(to, maxViewDistance);

        nearbyIds.stream()
                .map(id -> manager.getHologram(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
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

        Set<String> nearbyGlobalIds = manager.getVisibilityManager()
                .getSpatialChunkManager()
                .getNearbyHologramIds(to, maxViewDistance);

        nearbyGlobalIds.stream()
                .map(id -> manager.getHologram(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(h -> !h.isPerPlayer())
                .forEach(hologram -> {
                    boolean canSee = hologram.canSee(player);

                    if (canSee) {
                        for (org.bukkit.entity.TextDisplay display : hologram.getGlobalDisplays()) {
                            if (!player.canSee(display)) {
                                player.showEntity(manager.getPlugin(), display);
                            }
                        }
                    } else {
                        for (org.bukkit.entity.TextDisplay display : hologram.getGlobalDisplays()) {
                            if (player.canSee(display)) {
                                player.hideEntity(manager.getPlugin(), display);
                            }
                        }
                    }
                });
    }
}
