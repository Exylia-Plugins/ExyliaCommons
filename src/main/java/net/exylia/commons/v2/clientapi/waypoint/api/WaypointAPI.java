package net.exylia.commons.v2.clientapi.waypoint.api;

import net.exylia.commons.v2.clientapi.waypoint.core.WaypointManager;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public final class WaypointAPI {

    private static WaypointManager manager;

    private WaypointAPI() {}

    public static void initialize(WaypointManager waypointManager) {
        manager = waypointManager;
    }

    public static UUID show(Player player, WaypointDefinition definition) {
        if (manager == null) return null;
        return manager.show(player, definition);
    }

    public static void show(Collection<? extends Player> players, WaypointDefinition definition) {
        if (manager == null) return;
        for (Player player : players) {
            manager.show(player, definition);
        }
    }

    public static UUID showPersistent(Player player, WaypointDefinition definition) {
        if (manager == null) return null;
        return manager.showPersistent(player, definition);
    }

    public static void showPersistent(Collection<? extends Player> players, WaypointDefinition definition) {
        if (manager == null) return;
        for (Player player : players) {
            manager.showPersistent(player, definition);
        }
    }

    public static void remove(Player player, UUID trackingId) {
        if (manager == null) return;
        manager.remove(player, trackingId);
    }

    public static void removePersistent(Player player, UUID persistentId) {
        if (manager == null) return;
        manager.removePersistent(player, persistentId);
    }

    public static void removePersistent(UUID playerUUID, UUID persistentId) {
        if (manager == null) return;
        manager.removePersistent(playerUUID, persistentId);
    }

    public static void removeAll(Player player) {
        if (manager == null) return;
        manager.removeAll(player);
    }

    public static void removeAll(Collection<? extends Player> players) {
        if (manager == null) return;
        for (Player player : players) {
            manager.removeAll(player);
        }
    }
}
