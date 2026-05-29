package net.exylia.commons.v2.clientapi.waypoint.core;

import net.exylia.commons.v2.clientapi.waypoint.adapter.WaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.adapter.impl.ApolloWaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.adapter.impl.FeatherWaypointAdapter;
import net.exylia.commons.v2.clientapi.waypoint.model.WaypointDefinition;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager {

    private final List<WaypointAdapter> adapters = new ArrayList<>();

    // What SHOULD be shown per player (session-scoped, cleared on quit)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, WaypointDefinition>> desiredState = new ConcurrentHashMap<>();

    // Persistent waypoints survive reconnect
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, WaypointDefinition>> persistentStore = new ConcurrentHashMap<>();

    // Players queued for reconcile on next scheduled pass
    private final Set<UUID> pendingReconcile = ConcurrentHashMap.newKeySet();

    // Handles we currently have displayed per player per adapter
    // Map<playerUUID, Map<waypointId, Map<adapterSimpleName, handle>>>
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>>> adapterHandles = new ConcurrentHashMap<>();

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
            adapters.add(new ApolloWaypointAdapter());
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
            adapters.add(new FeatherWaypointAdapter());
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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public UUID show(Player player, WaypointDefinition definition) {
        UUID id = UUID.randomUUID();
        removeByName(player.getUniqueId(), definition.getName());
        desiredState.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, definition);
        pendingReconcile.add(player.getUniqueId());
        DebugAPI.logLibDebug("[Waypoint] show() player=" + player.getName() + " name=" + definition.getName()
                + " worldId=" + definition.getWorldId() + " worldName=" + definition.getWorldName() + " id=" + id);
        return id;
    }

    public UUID showPersistent(Player player, WaypointDefinition definition) {
        UUID id = UUID.randomUUID();
        removeByName(player.getUniqueId(), definition.getName());
        desiredState.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, definition);
        persistentStore.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, definition);
        pendingReconcile.add(player.getUniqueId());
        DebugAPI.logLibDebug("[Waypoint] showPersistent() player=" + player.getName() + " name=" + definition.getName()
                + " worldId=" + definition.getWorldId() + " worldName=" + definition.getWorldName() + " id=" + id);
        return id;
    }

    public void remove(Player player, UUID waypointId) {
        remove(player.getUniqueId(), waypointId);
    }

    public void removePersistent(Player player, UUID waypointId) {
        remove(player.getUniqueId(), waypointId);
    }

    public void removePersistent(UUID playerUUID, UUID waypointId) {
        remove(playerUUID, waypointId);
    }

    public void removeAll(Player player) {
        desiredState.remove(player.getUniqueId());
        persistentStore.remove(player.getUniqueId());
        pendingReconcile.remove(player.getUniqueId());
        DebugAPI.logLibDebug("[Waypoint] removeAll() player=" + player.getName());
        removeTrackedHandles(player);
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------

    public void handleJoin(Player player) {
        // Client reconnected — no stale state on their side
        adapterHandles.remove(player.getUniqueId());
        Map<UUID, WaypointDefinition> persistent = persistentStore.get(player.getUniqueId());
        int count = persistent != null ? persistent.size() : 0;
        DebugAPI.logLibDebug("[Waypoint] handleJoin() player=" + player.getName() + " persistentWaypoints=" + count);
        if (persistent != null && !persistent.isEmpty()) {
            desiredState.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).putAll(persistent);
        }
        pendingReconcile.add(player.getUniqueId());
    }

    public void cleanupPlayer(Player player) {
        DebugAPI.logLibDebug("[Waypoint] cleanupPlayer() player=" + player.getName());
        desiredState.remove(player.getUniqueId());
        adapterHandles.remove(player.getUniqueId());
        pendingReconcile.remove(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Poller
    // -------------------------------------------------------------------------

    public void startPoller(Plugin plugin) {
        // Process pending reconciles (queued by show/remove calls) — runs every 2 ticks
        TaskAPI.syncTimer(() -> {
            if (pendingReconcile.isEmpty()) return;
            Set<UUID> batch = new HashSet<>(pendingReconcile);
            pendingReconcile.removeAll(batch);
            for (UUID uuid : batch) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    TaskAPI.at(player, () -> reconcile(player));
                }
            }
        }, 2L, 2L);

        // Full reconcile every 15 seconds — catches Folia world changes, drift, etc.
        TaskAPI.syncTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!desiredState.containsKey(player.getUniqueId())) continue;
                TaskAPI.at(player, () -> {
                    if (player.isOnline()) reconcile(player);
                });
            }
        }, 300L, 300L);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void remove(UUID playerUUID, UUID waypointId) {
        DebugAPI.logLibDebug("[Waypoint] remove() playerUUID=" + playerUUID + " waypointId=" + waypointId);
        Map<UUID, WaypointDefinition> desired = desiredState.get(playerUUID);
        if (desired != null) desired.remove(waypointId);
        Map<UUID, WaypointDefinition> persistent = persistentStore.get(playerUUID);
        if (persistent != null) persistent.remove(waypointId);
        pendingReconcile.add(playerUUID);
    }

    private void reconcile(Player player) {
        if (!player.isOnline()) return;

        Map<UUID, WaypointDefinition> desired = desiredState.get(player.getUniqueId());
        int desiredCount = desired != null ? desired.size() : 0;
        UUID currentWorldId = player.getWorld().getUID();
        String currentWorldName = player.getWorld().getName();
        DebugAPI.logLibDebug("[Waypoint] reconcile() player=" + player.getName()
                + " world=" + currentWorldName + " worldId=" + currentWorldId
                + " desired=" + desiredCount + " adapters=" + adapters.size());

        // Remove only the waypoints WE previously sent — non-destructive to other plugins
        removeTrackedHandles(player);

        // Remove adapter-specific auto-generated waypoints (e.g., Apollo's built-in "Spawn")
        for (WaypointAdapter adapter : adapters) {
            if (!adapter.isAvailable() || !adapter.supportsPlayer(player)) continue;
            for (String handle : adapter.autoRemovedHandles()) {
                try { adapter.remove(player, handle); } catch (Exception ignored) {}
            }
        }

        if (desired == null || desired.isEmpty()) {
            DebugAPI.logLibDebug("[Waypoint] reconcile() player=" + player.getName() + " no desired waypoints, skipping send");
            return;
        }

        int sent = 0;
        int skippedWorld = 0;
        int skippedAdapter = 0;

        for (Map.Entry<UUID, WaypointDefinition> entry : desired.entrySet()) {
            UUID waypointId = entry.getKey();
            WaypointDefinition definition = entry.getValue();

            UUID waypointWorldId = definition.getWorldId();
            String waypointWorldName = definition.getWorldName();
            if (waypointWorldId != null && !waypointWorldId.equals(currentWorldId)) {
                DebugAPI.logLibDebug("[Waypoint] reconcile() SKIP player=" + player.getName()
                        + " name=" + definition.getName() + " reason=worldId"
                        + " waypointWorld=" + waypointWorldId + " playerWorld=" + currentWorldId);
                skippedWorld++;
                continue;
            }
            if (waypointWorldId == null && waypointWorldName != null && !waypointWorldName.equals(currentWorldName)) {
                DebugAPI.logLibDebug("[Waypoint] reconcile() SKIP player=" + player.getName()
                        + " name=" + definition.getName() + " reason=worldName"
                        + " waypointWorld=" + waypointWorldName + " playerWorld=" + currentWorldName);
                skippedWorld++;
                continue;
            }

            for (WaypointAdapter adapter : adapters) {
                if (!adapter.isAvailable()) continue;
                if (!adapter.supportsPlayer(player)) {
                    DebugAPI.logLibDebug("[Waypoint] reconcile() SKIP_ADAPTER player=" + player.getName()
                            + " name=" + definition.getName() + " adapter=" + adapter.getClass().getSimpleName());
                    skippedAdapter++;
                    continue;
                }
                try {
                    String handle = adapter.show(player, definition);
                    if (handle != null) {
                        adapterHandles
                                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(waypointId, k -> new ConcurrentHashMap<>())
                                .put(adapter.getClass().getSimpleName(), handle);
                    }
                    sent++;
                } catch (Exception e) {
                    DebugAPI.logLibError("Error applying waypoint via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        DebugAPI.logLibDebug("[Waypoint] reconcile() done player=" + player.getName()
                + " sent=" + sent + " skippedWorld=" + skippedWorld + " skippedAdapter=" + skippedAdapter);
    }

    private void removeTrackedHandles(Player player) {
        ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> playerHandles = adapterHandles.remove(player.getUniqueId());
        if (playerHandles == null) return;
        DebugAPI.logLibDebug("[Waypoint] removeTrackedHandles() player=" + player.getName() + " waypoints=" + playerHandles.size());
        for (ConcurrentHashMap<String, String> adapterMap : playerHandles.values()) {
            for (Map.Entry<String, String> entry : adapterMap.entrySet()) {
                WaypointAdapter adapter = findAdapterBySimpleName(entry.getKey());
                if (adapter == null || !adapter.isAvailable()) continue;
                try { adapter.remove(player, entry.getValue()); } catch (Exception ignored) {}
            }
        }
    }

    private WaypointAdapter findAdapterBySimpleName(String simpleName) {
        for (WaypointAdapter adapter : adapters) {
            if (adapter.getClass().getSimpleName().equals(simpleName)) return adapter;
        }
        return null;
    }

    private void removeByName(UUID playerUUID, String name) {
        ConcurrentHashMap<UUID, WaypointDefinition> desired = desiredState.get(playerUUID);
        if (desired != null) desired.entrySet().removeIf(e -> name.equals(e.getValue().getName()));
        ConcurrentHashMap<UUID, WaypointDefinition> persistent = persistentStore.get(playerUUID);
        if (persistent != null) persistent.entrySet().removeIf(e -> name.equals(e.getValue().getName()));
    }

    public List<WaypointAdapter> getAdapters() {
        return Collections.unmodifiableList(adapters);
    }
}
