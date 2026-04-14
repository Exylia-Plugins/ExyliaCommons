package net.exylia.commons.v2.clientapi.waypoint.core;

import net.exylia.commons.v2.clientapi.waypoint.adapter.WaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.adapter.impl.ApolloWaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.adapter.impl.FeatherWaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager {

    private final List<WaypointAdapter> adapters = new ArrayList<>();
    private final ConcurrentHashMap<UUID, WaypointEntry> trackingMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<UUID>> playerTracking = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<UUID, WaypointDefinition>> persistentWaypoints = new ConcurrentHashMap<>();

    public void initialize() {
        tryRegisterApollo();
        tryRegisterFeather();
    }

    private void tryRegisterApollo() {
        if (!isAnyPluginEnabled("Apollo", "Apollo-Folia", "Apollo-Bukkit")) {
            DebugAPI.logLibWarn("Apollo (Lunar Client) API not found. ExyliaCommons supports it — add apollo-api as a dependency to enable it.");
            return;
        }
        try {
            ApolloWaypointAdapter adapter = new ApolloWaypointAdapter();
            adapters.add(adapter);
            DebugAPI.logLibSuccess("Apollo (Lunar Client) waypoint support enabled.");
        } catch (NoClassDefFoundError | Exception e) {
            DebugAPI.logLibError("Failed to initialize Apollo (Lunar Client) waypoint adapter: " + e.getMessage());
        }
    }

    private void tryRegisterFeather() {
        if (!isAnyPluginEnabled("FeatherServerAPI")) {
            DebugAPI.logLibWarn("Feather Client API not found. ExyliaCommons supports it — add feather-server-api as a dependency to enable it.");
            return;
        }
        try {
            FeatherWaypointAdapter adapter = new FeatherWaypointAdapter();
            adapters.add(adapter);
            DebugAPI.logLibSuccess("Feather Client waypoint support enabled.");
        } catch (NoClassDefFoundError | Exception e) {
            DebugAPI.logLibError("Failed to initialize Feather Client waypoint adapter: " + e.getMessage());
        }
    }

    private boolean isAnyPluginEnabled(String... names) {
        for (String name : names) {
            if (Bukkit.getPluginManager().isPluginEnabled(name)) return true;
        }
        return false;
    }

    public void registerAdapter(WaypointAdapter adapter) {
        if (adapter == null || !adapter.isAvailable()) return;
        adapters.add(adapter);
        DebugAPI.logLibSuccess("Registered waypoint adapter: " + adapter.getClass().getSimpleName());
    }

    public UUID show(Player player, WaypointDefinition definition) {
        UUID trackingId = UUID.randomUUID();
        sendToAdapters(trackingId, player, definition);
        return trackingId;
    }

    public UUID showPersistent(Player player, WaypointDefinition definition) {
        UUID id = UUID.randomUUID();
        persistentWaypoints.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, definition);
        sendToAdapters(id, player, definition);
        return id;
    }

    private void sendToAdapters(UUID id, Player player, WaypointDefinition definition) {
        WaypointEntry entry = new WaypointEntry(definition);
        for (WaypointAdapter adapter : adapters) {
            if (!adapter.isAvailable() || !adapter.supportsPlayer(player)) continue;
            try {
                String handle = adapter.show(player, definition);
                if (handle != null) entry.handles.put(adapter, handle);
            } catch (Exception e) {
                DebugAPI.logLibError("Error showing waypoint via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        trackingMap.put(id, entry);
        playerTracking.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    public void remove(Player player, UUID trackingId) {
        WaypointEntry entry = trackingMap.remove(trackingId);
        if (entry == null) return;

        Set<UUID> playerIds = playerTracking.get(player.getUniqueId());
        if (playerIds != null) playerIds.remove(trackingId);

        for (Map.Entry<WaypointAdapter, String> e : entry.handles.entrySet()) {
            if (!e.getKey().isAvailable()) continue;
            try {
                e.getKey().remove(player, e.getValue());
            } catch (Exception ex) {
                DebugAPI.logLibError("Error removing waypoint via " + e.getKey().getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    }

    public void removePersistent(Player player, UUID persistentId) {
        removePersistent(player.getUniqueId(), persistentId);
    }

    public void removePersistent(UUID playerUUID, UUID persistentId) {
        Map<UUID, WaypointDefinition> persistent = persistentWaypoints.get(playerUUID);
        if (persistent != null) persistent.remove(persistentId);

        WaypointEntry entry = trackingMap.remove(persistentId);
        Set<UUID> playerIds = playerTracking.get(playerUUID);
        if (playerIds != null) playerIds.remove(persistentId);

        if (entry == null) return;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;

        for (Map.Entry<WaypointAdapter, String> e : entry.handles.entrySet()) {
            if (!e.getKey().isAvailable()) continue;
            try {
                e.getKey().remove(player, e.getValue());
            } catch (Exception ex) {
                DebugAPI.logLibError("Error removing persistent waypoint: " + ex.getMessage());
            }
        }
    }

    public void removeAll(Player player) {
        persistentWaypoints.remove(player.getUniqueId());
        clearSessionTracking(player);
        for (WaypointAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;
            try {
                adapter.removeAll(player);
            } catch (Exception e) {
                DebugAPI.logLibError("Error removing all waypoints via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void handleJoin(Player player) {
        Map<UUID, WaypointDefinition> persistent = persistentWaypoints.get(player.getUniqueId());
        if (persistent == null || persistent.isEmpty()) return;
        for (Map.Entry<UUID, WaypointDefinition> entry : persistent.entrySet()) {
            sendToAdapters(entry.getKey(), player, entry.getValue());
        }
    }

    public void handleWorldChange(Player player) {
        Set<UUID> trackingIds = playerTracking.get(player.getUniqueId());
        if (trackingIds == null || trackingIds.isEmpty()) return;
        for (UUID trackingId : new HashSet<>(trackingIds)) {
            WaypointEntry entry = trackingMap.get(trackingId);
            if (entry == null) continue;
            if (entry.definition.getWorldId() != null) continue;
            for (WaypointAdapter adapter : adapters) {
                if (!adapter.isAvailable() || !adapter.needsResendOnWorldChange()) continue;
                String oldHandle = entry.handles.remove(adapter);
                if (oldHandle != null) {
                    try { adapter.remove(player, oldHandle); } catch (Exception ignored) {}
                }
                if (!adapter.supportsPlayer(player)) continue;
                try {
                    String newHandle = adapter.show(player, entry.definition);
                    if (newHandle != null) entry.handles.put(adapter, newHandle);
                } catch (Exception e) {
                    DebugAPI.logLibError("Error resending waypoint on world change: " + e.getMessage());
                }
            }
        }
    }

    public void cleanupPlayer(Player player) {
        clearSessionTracking(player);
    }

    private void clearSessionTracking(Player player) {
        Set<UUID> trackingIds = playerTracking.remove(player.getUniqueId());
        if (trackingIds != null) {
            for (UUID id : trackingIds) trackingMap.remove(id);
        }
    }

    public List<WaypointAdapter> getAdapters() {
        return Collections.unmodifiableList(adapters);
    }

    private static final class WaypointEntry {
        final WaypointDefinition definition;
        final Map<WaypointAdapter, String> handles = new ConcurrentHashMap<>();

        WaypointEntry(WaypointDefinition definition) {
            this.definition = definition;
        }
    }
}
