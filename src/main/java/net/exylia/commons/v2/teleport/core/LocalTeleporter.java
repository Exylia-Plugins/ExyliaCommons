package net.exylia.commons.v2.teleport.core;

import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class LocalTeleporter {

    private final Plugin plugin;
    private final List<Consumer<Player>> postTeleportHooks;

    LocalTeleporter(Plugin plugin, List<Consumer<Player>> postTeleportHooks) {
        this.plugin = plugin;
        this.postTeleportHooks = postTeleportHooks;
    }

    CompletableFuture<Boolean> teleport(Player player, Location location) {
        if (!player.isOnline()) return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> result;
        if (TaskAPI.isFolia()) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            player.getScheduler().run(plugin, t ->
                    player.teleportAsync(location).thenAccept(future::complete),
                    () -> future.complete(false));
            result = future;
        } else {
            result = player.teleportAsync(location);
        }

        return result.thenApply(success -> {
            if (success) fireHooks(player);
            return success;
        });
    }

    private void fireHooks(Player player) {
        for (Consumer<Player> hook : postTeleportHooks) {
            try { hook.accept(player); } catch (Exception ignored) {}
        }
    }

    CompletableFuture<Void> teleportAll(Collection<? extends Player> players, Location location) {
        if (players.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<?>[] futures = players.stream()
                .filter(Player::isOnline)
                .map(p -> teleport(p, location))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
}
