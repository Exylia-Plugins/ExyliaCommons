package net.exylia.commons.v2.hologram.visibility;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.cache.HologramCacheManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VisibilityManager {
    private final HologramCacheManager cacheManager;
    private final ViewDistanceTracker distanceTracker = new ViewDistanceTracker();

    public Set<Player> getVisiblePlayers(Hologram hologram) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> canSee(p, hologram))
                .collect(Collectors.toSet());
    }

    public boolean canSee(Player player, Hologram hologram) {
        if (!player.getWorld().equals(hologram.getLocation().getWorld())) {
            return false;
        }

        double distance = player.getLocation().distance(hologram.getLocation());
        if (distance > hologram.getViewDistance()) {
            return false;
        }

        if (hologram.getVisibilityCondition() != null) {
            return hologram.getVisibilityCondition().canSee(player, hologram);
        }

        return true;
    }

    public void updateVisibility(Hologram hologram) {
        if (!hologram.isPerPlayer()) {
            return;
        }

        Set<Player> shouldSee = getVisiblePlayers(hologram);
        Set<UUID> currentlySee = hologram.getPlayerDisplays().keySet();

        shouldSee.stream()
                .filter(p -> !currentlySee.contains(p.getUniqueId()))
                .forEach(hologram::showTo);

        currentlySee.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(p -> !shouldSee.contains(p))
                .forEach(hologram::hideFrom);
    }

    public void showToPlayer(Player player, Hologram hologram) {
        if (!canSee(player, hologram)) {
            return;
        }

        hologram.showTo(player);

        cacheManager.getPlayerVisibility(player.getUniqueId())
                .ifPresentOrElse(
                        set -> set.add(hologram.getId()),
                        () -> {
                            Set<String> newSet = new HashSet<>();
                            newSet.add(hologram.getId());
                            cacheManager.cachePlayerVisibility(player.getUniqueId(), newSet);
                        }
                );
    }

    public void hideFromPlayer(Player player, Hologram hologram) {
        hologram.hideFrom(player);

        cacheManager.getPlayerVisibility(player.getUniqueId())
                .ifPresent(set -> set.remove(hologram.getId()));
    }

    public void trackPlayer(Player player) {
        distanceTracker.trackPlayer(player);
    }

    public void untrackPlayer(Player player) {
        distanceTracker.untrackPlayer(player);
        cacheManager.invalidatePlayer(player.getUniqueId());
    }

    public void updatePlayerLocation(Player player) {
        distanceTracker.updatePlayerLocation(player);
    }

    public Set<Player> getPlayersInRange(Hologram hologram) {
        return distanceTracker.getPlayersInRange(
                hologram.getLocation(),
                hologram.getViewDistance()
        );
    }

    public ViewDistanceTracker getDistanceTracker() {
        return distanceTracker;
    }
}
