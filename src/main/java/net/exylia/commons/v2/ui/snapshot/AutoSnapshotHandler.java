package net.exylia.commons.v2.ui.snapshot;

import net.exylia.commons.v2.snapshot.api.SnapshotAPI;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSnapshotHandler {

    private static final Map<UUID, String> activeSnapshots = new ConcurrentHashMap<>();

    public static CompletableFuture<SnapshotData> createSnapshot(Player player, String snapshotId) {
        if (snapshotId == null) {
            snapshotId = "menu_" + UUID.randomUUID();
        }

        String finalSnapshotId = snapshotId;
        activeSnapshots.put(player.getUniqueId(), finalSnapshotId);

        return SnapshotAPI.createAndRegisterAsync(player, finalSnapshotId);
    }

    public static CompletableFuture<Boolean> restoreSnapshot(Player player) {
        String snapshotId = activeSnapshots.get(player.getUniqueId());
        if (snapshotId == null) {
            return CompletableFuture.completedFuture(false);
        }

        return SnapshotAPI.restoreRegisteredAsync(player, snapshotId)
                .thenApply(result -> {
                    SnapshotAPI.unregister(player.getUniqueId(), snapshotId);
                    activeSnapshots.remove(player.getUniqueId());
                    return result;
                });
    }

    public static void clearSnapshot(Player player) {
        String snapshotId = activeSnapshots.remove(player.getUniqueId());
        if (snapshotId != null) {
            SnapshotAPI.unregister(player.getUniqueId(), snapshotId);
        }
    }

    public static boolean hasSnapshot(Player player) {
        return activeSnapshots.containsKey(player.getUniqueId());
    }
}
