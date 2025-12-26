package net.exylia.commons.v2.snapshot.core;

import net.exylia.commons.v2.snapshot.model.SnapshotData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotRegistry {

    private final Map<String, SnapshotData> snapshots = new ConcurrentHashMap<>();

    public void register(UUID playerUuid, String snapshotId, SnapshotData snapshot) {
        snapshots.put(buildKey(playerUuid, snapshotId), snapshot);
    }

    public void unregister(UUID playerUuid, String snapshotId) {
        snapshots.remove(buildKey(playerUuid, snapshotId));
    }

    public Optional<SnapshotData> get(UUID playerUuid, String snapshotId) {
        return Optional.ofNullable(snapshots.get(buildKey(playerUuid, snapshotId)));
    }

    public void clear() {
        snapshots.clear();
    }

    public int size() {
        return snapshots.size();
    }

    private String buildKey(UUID playerUuid, String snapshotId) {
        return playerUuid.toString() + ":" + snapshotId;
    }
}
