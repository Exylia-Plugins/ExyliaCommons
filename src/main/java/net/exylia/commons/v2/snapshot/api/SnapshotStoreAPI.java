package net.exylia.commons.v2.snapshot.api;

import net.exylia.commons.v2.snapshot.store.SnapshotStore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class SnapshotStoreAPI {

    private SnapshotStoreAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize() {
        SnapshotStore.getInstance();
    }

    public static CompletableFuture<Void> save(Player player, String contextId) {
        return SnapshotStore.getInstance().save(player, contextId);
    }

    public static CompletableFuture<Void> saveAndClear(Player player, String contextId) {
        return SnapshotStore.getInstance().saveAndClear(player, contextId);
    }

    public static CompletableFuture<Void> restore(Player player, Consumer<Location> teleportCallback) {
        return SnapshotStore.getInstance().restore(player, teleportCallback);
    }

    public static void restoreSync(Player player, Consumer<Location> teleportCallback) {
        SnapshotStore.getInstance().restoreSync(player, teleportCallback);
    }

    public static void checkAndRestore(Player player, Consumer<Location> teleportCallback) {
        SnapshotStore.getInstance().checkAndRestore(player, teleportCallback);
    }
}
