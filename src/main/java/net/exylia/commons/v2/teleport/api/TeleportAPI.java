package net.exylia.commons.v2.teleport.api;

import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.teleport.core.TeleportManager;
import net.exylia.commons.v2.teleport.listener.TeleportJoinListener;
import net.exylia.commons.v2.teleport.model.ExyliaLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TeleportAPI {

    private static TeleportManager manager;

    private TeleportAPI() {}

    public static void initialize(Plugin plugin) {
        if (manager != null) throw new IllegalStateException("TeleportAPI is already initialized.");
        DatabaseManager db = DatabaseManager.getInstance();
        manager = new TeleportManager(
                plugin,
                db.getConfig().getServerId(),
                db.getRedisPool(),
                db.getConfig().getRedisConfig().getKeyPrefix()
        );
        plugin.getServer().getPluginManager().registerEvents(new TeleportJoinListener(manager), plugin);
    }

    public static boolean isInitialized() {
        return manager != null;
    }

    public static void addPostTeleportHook(Consumer<Player> hook) {
        manager.addPostTeleportHook(hook);
    }

    public static void shutdown() {
        manager = null;
    }

    public static CompletableFuture<Boolean> teleport(Player player, Location location) {
        return manager.teleport(player, location);
    }

    public static CompletableFuture<Boolean> teleport(Player player, ExyliaLocation destination) {
        return manager.teleport(player, destination);
    }

    public static CompletableFuture<Boolean> teleport(Player player, String serialized) {
        return manager.teleport(player, serialized);
    }

    public static CompletableFuture<Void> teleport(Collection<? extends Player> players, Location location) {
        return manager.teleport(players, location);
    }

    public static CompletableFuture<Void> teleport(Collection<? extends Player> players, ExyliaLocation destination) {
        return manager.teleport(players, destination);
    }

    public static CompletableFuture<Void> teleport(Collection<? extends Player> players, String serialized) {
        return manager.teleport(players, serialized);
    }
}
