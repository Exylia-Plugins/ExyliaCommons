package net.exylia.commons.v2.snapshot.store;

import net.exylia.commons.v2.database.api.Database;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SnapshotStore {

    private static volatile SnapshotStore instance;

    private final Repository<PlayerStateRecord> repo;
    private final ConcurrentHashMap<UUID, Long> restoreGeneration = new ConcurrentHashMap<>();

    private SnapshotStore() {
        Database.registerEntity(PlayerStateRecord.class);
        this.repo = Database.getRepository(PlayerStateRecord.class);
    }

    public static SnapshotStore getInstance() {
        if (instance == null) {
            synchronized (SnapshotStore.class) {
                if (instance == null) {
                    instance = new SnapshotStore();
                }
            }
        }
        return instance;
    }

    public CompletableFuture<Void> save(Player player, String contextId) {
        restoreGeneration.merge(player.getUniqueId(), 1L, Long::sum);
        SnapshotData snapshot = SnapshotData.fromPlayer(player);
        Location loc = player.getLocation();
        PlayerStateRecord record = new PlayerStateRecord(player.getUniqueId(), snapshot, contextId, loc);
        return Tasks.dbRun(() -> repo.save(record));
    }

    public CompletableFuture<Void> saveAndClear(Player player, String contextId) {
        restoreGeneration.merge(player.getUniqueId(), 1L, Long::sum);

        SnapshotData snapshot = SnapshotData.fromPlayer(player);
        Location loc = player.getLocation();
        PlayerStateRecord record = new PlayerStateRecord(player.getUniqueId(), snapshot, contextId, loc);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        return Tasks.dbRun(() -> repo.save(record));
    }

    public CompletableFuture<Void> restore(Player player, Consumer<Location> teleportCallback) {
        UUID uuid = player.getUniqueId();
        long gen = restoreGeneration.getOrDefault(uuid, 0L);

        return Tasks.dbValue(() -> repo.findById(uuid.toString()))
            .thenAccept(opt -> {
                if (opt == null || opt.isEmpty()) return;
                if (restoreGeneration.getOrDefault(uuid, 0L) != gen) return;
                PlayerStateRecord record = opt.get();
                Tasks.runOnEntity(player, () -> {
                    if (!player.isOnline()) return;
                    if (restoreGeneration.getOrDefault(uuid, 0L) != gen) return;
                    record.getSnapshot().applyToPlayer(player);
                    if (teleportCallback != null) teleportCallback.accept(record.getLastLocation());
                    restoreGeneration.remove(uuid);
                    Tasks.dbRun(() -> repo.delete(record));
                });
            });
    }

    public void restoreSync(Player player, Consumer<Location> teleportCallback) {
        UUID uuid = player.getUniqueId();
        Optional<PlayerStateRecord> opt = repo.findById(uuid.toString());
        if (opt.isPresent()) {
            PlayerStateRecord record = opt.get();
            if (player.isOnline()) {
                record.getSnapshot().applyToPlayer(player);
                if (teleportCallback != null) teleportCallback.accept(record.getLastLocation());
            }
            repo.delete(record);
            restoreGeneration.remove(uuid);
        }
    }

    public void checkAndRestore(Player player, Consumer<Location> teleportCallback) {
        UUID uuid = player.getUniqueId();
        long gen = restoreGeneration.getOrDefault(uuid, 0L);

        Tasks.dbValue(() -> repo.findById(uuid.toString()))
            .thenAccept(opt -> {
                if (opt == null || opt.isEmpty()) return;
                if (restoreGeneration.getOrDefault(uuid, 0L) != gen) return;
                PlayerStateRecord record = opt.get();
                Tasks.runOnEntity(player, () -> {
                    if (!player.isOnline()) return;
                    if (restoreGeneration.getOrDefault(uuid, 0L) != gen) return;
                    record.getSnapshot().applyToPlayer(player);
                    if (teleportCallback != null) teleportCallback.accept(record.getLastLocation());
                    restoreGeneration.remove(uuid);
                    Tasks.dbRun(() -> repo.delete(record));
                });
            });
    }
}
