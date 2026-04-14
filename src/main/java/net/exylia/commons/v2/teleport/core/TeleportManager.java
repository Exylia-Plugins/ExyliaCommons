package net.exylia.commons.v2.teleport.core;

import net.exylia.commons.v2.database.redis.RedisConnectionPool;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.teleport.model.ExyliaLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class TeleportManager {

    private final String currentServer;
    private final LocalTeleporter local;
    private final CrossServerTeleporter crossServer;

    public TeleportManager(Plugin plugin, String currentServer, RedisConnectionPool redisPool, String keyPrefix) {
        this.currentServer = currentServer;
        this.local = new LocalTeleporter(plugin);
        this.crossServer = redisPool != null ? new CrossServerTeleporter(plugin, redisPool, keyPrefix) : null;
    }

    public CompletableFuture<Boolean> teleport(Player player, Location location) {
        return local.teleport(player, location);
    }

    public CompletableFuture<Boolean> teleport(Player player, ExyliaLocation destination) {
        if (!destination.isSameServer(currentServer)) {
            if (crossServer == null) {
                DebugAPI.logLibWarn("Cross-server teleport skipped for " + player.getName() + ": Redis not configured.");
                return CompletableFuture.completedFuture(false);
            }
            crossServer.teleport(player, destination);
            return CompletableFuture.completedFuture(true);
        }
        Location loc = destination.toBukkitLocation();
        if (loc == null) {
            DebugAPI.logLibWarn("Teleport failed for " + player.getName() + ": world '" + destination.getWorld() + "' not found.");
            return CompletableFuture.completedFuture(false);
        }
        return local.teleport(player, loc);
    }

    public CompletableFuture<Boolean> teleport(Player player, String serialized) {
        try {
            return teleport(player, ExyliaLocation.fromString(serialized));
        } catch (IllegalArgumentException e) {
            DebugAPI.logLibWarn("Invalid ExyliaLocation for " + player.getName() + ": " + serialized);
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Void> teleport(Collection<? extends Player> players, Location location) {
        return local.teleportAll(players, location);
    }

    public CompletableFuture<Void> teleport(Collection<? extends Player> players, ExyliaLocation destination) {
        if (!destination.isSameServer(currentServer)) {
            if (crossServer == null) {
                DebugAPI.logLibWarn("Cross-server teleport skipped: Redis not configured.");
                return CompletableFuture.completedFuture(null);
            }
            crossServer.teleportAll(players, destination);
            return CompletableFuture.completedFuture(null);
        }
        Location loc = destination.toBukkitLocation();
        if (loc == null) {
            DebugAPI.logLibWarn("Teleport failed: world '" + destination.getWorld() + "' not found.");
            return CompletableFuture.completedFuture(null);
        }
        return local.teleportAll(players, loc);
    }

    public CompletableFuture<Void> teleport(Collection<? extends Player> players, String serialized) {
        try {
            return teleport(players, ExyliaLocation.fromString(serialized));
        } catch (IllegalArgumentException e) {
            DebugAPI.logLibWarn("Invalid ExyliaLocation string: " + serialized);
            return CompletableFuture.completedFuture(null);
        }
    }

    public void handlePendingTeleport(Player player) {
        if (crossServer == null) return;

        TaskAPI.async(() -> {
            Optional<ExyliaLocation> pending = crossServer.consumePending(player.getUniqueId());
            if (pending.isEmpty()) return null;

            ExyliaLocation destination = pending.get();
            TaskAPI.atLater(player, () -> {
                Location loc = destination.toBukkitLocation();
                if (loc == null) {
                    DebugAPI.logLibWarn("Pending teleport for " + player.getName() + ": world '" + destination.getWorld() + "' not found.");
                    return;
                }
                local.teleport(player, loc);
            }, 150L, TimeUnit.MILLISECONDS);
            return null;
        });
    }
}
