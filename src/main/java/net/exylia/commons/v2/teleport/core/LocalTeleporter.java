package net.exylia.commons.v2.teleport.core;

import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

final class LocalTeleporter {

    private final Plugin plugin;

    LocalTeleporter(Plugin plugin) {
        this.plugin = plugin;
    }

    CompletableFuture<Boolean> teleport(Player player, Location location) {
        if (!player.isOnline()) return CompletableFuture.completedFuture(false);

        if (TaskAPI.isFolia()) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            player.getScheduler().run(plugin, t ->
                    player.teleportAsync(location).thenAccept(future::complete),
                    () -> future.complete(false));
            return future;
        }

        return player.teleportAsync(location);
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
