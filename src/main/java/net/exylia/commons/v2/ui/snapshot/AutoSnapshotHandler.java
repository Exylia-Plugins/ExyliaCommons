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
        UUID playerId = player.getUniqueId();
        activeSnapshots.put(playerId, finalSnapshotId);

        return SnapshotAPI.createAndRegisterAsync(player, finalSnapshotId)
                .whenComplete((snapshot, throwable) -> {
                    if (throwable != null) {
                        activeSnapshots.remove(playerId);
                    }
                });
    }

    public static CompletableFuture<Boolean> restoreSnapshot(Player player) {
        UUID playerId = player.getUniqueId();
        String snapshotId = activeSnapshots.get(playerId);
        if (snapshotId == null) {
            return CompletableFuture.completedFuture(false);
        }

        return SnapshotAPI.restoreRegisteredAsync(player, snapshotId)
                .handle((result, throwable) -> {
                    SnapshotAPI.unregister(player.getUniqueId(), snapshotId);
                    activeSnapshots.remove(playerId);
                    if (throwable != null) {
                        return false;
                    }
                    return Boolean.TRUE.equals(result);
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
