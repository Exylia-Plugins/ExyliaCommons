package net.exylia.commons.v2.hologram.listener;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

        Collection<Hologram> all = manager.getAllHolograms();
        for (Hologram hologram : all) {
            if (hologram.isPerPlayer()) {
                if (hologram.canSee(player)) hologram.showTo(player);
            } else {
                org.bukkit.entity.TextDisplay display = hologram.getGlobalDisplay();
                if (!hologram.canSee(player) && display != null) {
                    TaskAPI.at(display, () -> player.hideEntity(manager.getPlugin(), display));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        for (Hologram hologram : manager.getAllHolograms()) {
            if (hologram.isPerPlayer()) hologram.cleanupPlayer(playerId);
        }

        manager.getVisibilityManager().untrackPlayer(player);
        lastUpdateLocation.remove(playerId);
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

        Set<String> nearbyIds = manager.getVisibilityManager()
                .getSpatialChunkManager()
                .getNearbyHologramIds(to, 64.0);

        for (String id : nearbyIds) {
            Hologram hologram = manager.getHologram(id).orElse(null);
            if (hologram == null) continue;

            if (hologram.isPerPlayer()) {
                boolean canSeeNow = hologram.canSee(player);
                boolean isSeeing = hologram.getPlayerDisplays().containsKey(playerId);
                if (canSeeNow && !isSeeing) {
                    hologram.showTo(player);
                } else if (!canSeeNow && isSeeing) {
                    hologram.hideFrom(player);
                }
            } else {
                org.bukkit.entity.TextDisplay display = hologram.getGlobalDisplay();
                if (display == null) continue;
                boolean canSee = hologram.canSee(player);
                if (canSee && !player.canSee(display)) {
                    TaskAPI.at(display, () -> player.showEntity(manager.getPlugin(), display));
                } else if (!canSee && player.canSee(display)) {
                    TaskAPI.at(display, () -> player.hideEntity(manager.getPlugin(), display));
                }
            }
        }
    }
}
