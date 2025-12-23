package net.exylia.commons.v2.hologram.visibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ViewDistanceTracker {
    private final Map<UUID, Location> playerLocations = new ConcurrentHashMap<>();

    public Set<Player> getPlayersInRange(Location location, double radius) {
        if (location.getWorld() == null) {
            return Collections.emptySet();
        }

        double radiusSquared = radius * radius;
        Set<Player> result = new HashSet<>();

        for (Map.Entry<UUID, Location> entry : playerLocations.entrySet()) {
            Location cachedLoc = entry.getValue();
            if (!cachedLoc.getWorld().equals(location.getWorld())) {
                continue;
            }

            if (cachedLoc.distanceSquared(location) <= radiusSquared) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    result.add(player);
                }
            }
        }

        return result;
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
