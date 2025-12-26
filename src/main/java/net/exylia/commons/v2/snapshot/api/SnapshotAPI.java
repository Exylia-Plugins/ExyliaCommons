package net.exylia.commons.v2.snapshot.api;

import net.exylia.commons.v2.snapshot.core.SnapshotManager;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SnapshotAPI {

    private SnapshotAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        SnapshotManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return SnapshotManager.isInitialized();
    }

    public static SnapshotBuilder builder(Player player) {
        return new SnapshotBuilder(player);
    }

    public static CompletableFuture<SnapshotData> createAsync(Player player) {
        return SnapshotManager.getInstance().createSnapshotAsync(player);
    }

    public static SnapshotData create(Player player) {
        return SnapshotManager.getInstance().createSnapshot(player);
    }

    public static CompletableFuture<SnapshotData> createAndRegisterAsync(Player player, String snapshotId) {
        return SnapshotManager.getInstance().createAndRegisterAsync(player, snapshotId);
    }

    public static SnapshotData createAndRegister(Player player, String snapshotId) {
        return SnapshotManager.getInstance().createAndRegister(player, snapshotId);
    }

    public static CompletableFuture<Boolean> restoreAsync(Player player, SnapshotData snapshot) {
        return SnapshotManager.getInstance().restoreAsync(player, snapshot);
    }

    public static void restore(Player player, SnapshotData snapshot) {
        SnapshotManager.getInstance().restore(player, snapshot);
    }

    public static CompletableFuture<Boolean> restoreRegisteredAsync(Player player, String snapshotId) {
        return SnapshotManager.getInstance().restoreRegisteredAsync(player, snapshotId);
    }

    public static boolean restoreRegistered(Player player, String snapshotId) {
        return SnapshotManager.getInstance().restoreRegistered(player, snapshotId);
    }

    public static Optional<SnapshotData> getRegistered(UUID playerUuid, String snapshotId) {
        return SnapshotManager.getInstance().getRegistered(playerUuid, snapshotId);
    }

    public static void register(UUID playerUuid, String snapshotId, SnapshotData snapshot) {
        SnapshotManager.getInstance().register(playerUuid, snapshotId, snapshot);
    }

    public static void unregister(UUID playerUuid, String snapshotId) {
        SnapshotManager.getInstance().unregister(playerUuid, snapshotId);
    }

    public static void shutdown() {
        SnapshotManager.getInstance().shutdown();
    }

    public static SnapshotManager getManager() {
        return SnapshotManager.getInstance();
    }
}
