package net.exylia.commons.v2.snapshot.exception;

import java.util.UUID;

public class SnapshotNotFoundException extends SnapshotException {

    public SnapshotNotFoundException(UUID playerUuid, String snapshotId) {
        super("Snapshot not found: " + playerUuid + ":" + snapshotId);
    }

    public SnapshotNotFoundException(String message) {
        super(message);
    }
}
