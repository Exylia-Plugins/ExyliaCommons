package net.exylia.commons.v2.snapshot.core;

import lombok.Getter;
import net.exylia.commons.v2.snapshot.cache.SnapshotCacheManager;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SnapshotManager {

    private static SnapshotManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;
    private final SnapshotRegistry registry;
    private final SnapshotCacheManager cacheManager;
    private final SnapshotFactory factory;

    private SnapshotManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registry = new SnapshotRegistry();
        this.cacheManager = new SnapshotCacheManager();
        this.factory = new SnapshotFactory();
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new SnapshotManager(plugin);
            }
        }
    }

    public static SnapshotManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SnapshotManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public CompletableFuture<SnapshotData> createSnapshotAsync(Player player) {
        return Tasks.entityValue(player, () -> SnapshotData.fromPlayer(player));
    }

    public SnapshotData createSnapshot(Player player) {
        return SnapshotData.fromPlayer(player);
    }

    public CompletableFuture<SnapshotData> createAndRegisterAsync(Player player, String snapshotId) {
        return createSnapshotAsync(player)
                .thenApply(snapshot -> {
                    registry.register(player.getUniqueId(), snapshotId, snapshot);
                    cacheManager.cache(player.getUniqueId(), snapshotId, snapshot);
                    return snapshot;
                });
    }

    public SnapshotData createAndRegister(Player player, String snapshotId) {
        SnapshotData snapshot = createSnapshot(player);
        registry.register(player.getUniqueId(), snapshotId, snapshot);
        cacheManager.cache(player.getUniqueId(), snapshotId, snapshot);
        return snapshot;
    }

    public CompletableFuture<Boolean> restoreAsync(Player player, SnapshotData snapshot) {
        return Tasks.entityValue(player, () -> {
            snapshot.applyToPlayer(player);
            return true;
        });
    }

    public void restore(Player player, SnapshotData snapshot) {
        snapshot.applyToPlayer(player);
    }

    public CompletableFuture<Boolean> restoreRegisteredAsync(Player player, String snapshotId) {
        Optional<SnapshotData> snapshot = getRegistered(player.getUniqueId(), snapshotId);
        if (snapshot.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return Tasks.entityValue(player, () -> {
            snapshot.get().applyToPlayer(player);
            return true;
        });
    }

    public boolean restoreRegistered(Player player, String snapshotId) {
        Optional<SnapshotData> snapshot = getRegistered(player.getUniqueId(), snapshotId);
        if (snapshot.isEmpty()) {
            return false;
        }

        snapshot.get().applyToPlayer(player);
        return true;
    }

    public Optional<SnapshotData> getRegistered(UUID playerUuid, String snapshotId) {
        Optional<SnapshotData> cached = cacheManager.get(playerUuid, snapshotId);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<SnapshotData> registered = registry.get(playerUuid, snapshotId);
        registered.ifPresent(snapshot -> cacheManager.cache(playerUuid, snapshotId, snapshot));
        return registered;
    }

    public void register(UUID playerUuid, String snapshotId, SnapshotData snapshot) {
        registry.register(playerUuid, snapshotId, snapshot);
        cacheManager.cache(playerUuid, snapshotId, snapshot);
    }

    public void unregister(UUID playerUuid, String snapshotId) {
        registry.unregister(playerUuid, snapshotId);
        cacheManager.invalidate(playerUuid, snapshotId);
    }

    public void shutdown() {
        registry.clear();
        cacheManager.invalidateAll();

        synchronized (LOCK) {
            instance = null;
        }
    }

    public SnapshotRegistry getRegistry() {
        return registry;
    }

    public SnapshotCacheManager getCacheManager() {
        return cacheManager;
    }

    public SnapshotFactory getFactory() {
        return factory;
    }
}
