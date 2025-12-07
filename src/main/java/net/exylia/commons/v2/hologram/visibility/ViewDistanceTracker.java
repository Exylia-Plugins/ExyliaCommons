package net.exylia.commons.v2.hologram.visibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ViewDistanceTracker {
    private final Map<UUID, Location> playerLocations = new ConcurrentHashMap<>();

    public Set<Player> getPlayersInRange(Location location, double radius) {
        if (location.getWorld() == null) {
            return Collections.emptySet();
        }

        double radiusSquared = radius * radius;

        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().equals(location.getWorld()))
                .filter(player -> {
                    Location playerLoc = getPlayerLocation(player);
                    return playerLoc.distanceSquared(location) <= radiusSquared;
                })
                .collect(Collectors.toSet());
    }

    public void trackPlayer(Player player) {
        playerLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void untrackPlayer(Player player) {
        playerLocations.remove(player.getUniqueId());
    }

    public void updatePlayerLocation(Player player) {
        playerLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public Location getPlayerLocation(Player player) {
        Location cached = playerLocations.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }

        Location current = player.getLocation();
        playerLocations.put(player.getUniqueId(), current.clone());
        return current;
    }

    public boolean isInRange(Player player, Location location, double radius) {
        if (!player.getWorld().equals(location.getWorld())) {
            return false;
        }

        Location playerLoc = getPlayerLocation(player);
        return playerLoc.distanceSquared(location) <= (radius * radius);
    }

    public void clear() {
        playerLocations.clear();
    }

    public int getTrackedPlayerCount() {
        return playerLocations.size();
    }
}
