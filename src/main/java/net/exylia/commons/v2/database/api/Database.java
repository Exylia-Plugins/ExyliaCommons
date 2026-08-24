package net.exylia.commons.v2.database.api;

import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.database.annotation.PlayerSession;
import net.exylia.commons.v2.database.config.DatabaseDefaults;
import net.exylia.commons.v2.database.core.DatabaseManager;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.repository.Repository;
import net.exylia.commons.v2.database.repository.WriteBehindRepository;
import net.exylia.commons.v2.database.transfer.api.DatabaseTransferAPI;
import net.exylia.commons.v2.database.transfer.model.TransferResult;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Database {

    private static final ConcurrentHashMap<Class<?>, WriteBehindRepository<?>> writeBehindCache = new ConcurrentHashMap<>();

    private Database() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(Plugin plugin) {
        DatabaseManager.initialize(Configs.get("database"), plugin);
    }

    public static <T extends Entity> void registerEntity(Class<T> entityClass) {
        DatabaseManager.getInstance().registerEntity(entityClass);
    }

    public static <T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass) {
        return DatabaseManager.getInstance().registerEntityAsync(entityClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> Repository<T> getRepository(Class<T> entityClass) {
        if (!DatabaseDefaults.Database.WriteBehind.ENABLED) {
            DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Disabled — returning direct repository for " + entityClass.getSimpleName());
            return DatabaseManager.getInstance().getRepository(entityClass);
        }
        return (WriteBehindRepository<T>) writeBehindCache.computeIfAbsent(
                entityClass,
                cls -> new WriteBehindRepository<>(
                        DatabaseManager.getInstance().getRepository(entityClass),
                        DatabaseDefaults.Database.WriteBehind.FLUSH_INTERVAL
                )
        );
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Void> flushPlayerSession(UUID playerUuid) {
        DatabaseManager manager = DatabaseManager.getInstance();
        Set<Class<? extends Entity>> sessionEntities = manager.getSessionEntities();
        if (sessionEntities.isEmpty()) return CompletableFuture.completedFuture(null);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Class<? extends Entity> entityClass : sessionEntities) {
            Repository<Entity> repo = DatabaseDefaults.Database.WriteBehind.ENABLED
                    ? (Repository<Entity>) writeBehindCache.get(entityClass)
                    : (Repository<Entity>) manager.getRepository(entityClass);
            if (repo == null) continue;

            PlayerSession annotation = entityClass.getAnnotation(PlayerSession.class);
            String playerField = annotation.playerField();

            if (playerField.isEmpty()) {
                Object id = manager.resolvePlayerSessionId(entityClass, playerUuid);
                futures.add(repo.flushEntity(id));
            } else {
                Object fieldValue = manager.resolvePlayerFieldValue(entityClass, playerField, playerUuid);
                futures.add(repo.flushEntitiesBy(playerField, fieldValue));
            }
        }
        return futures.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Forces every buffered write to disk and blocks until it is durable.
     * Returns the number of repositories that failed, so a caller about to restart
     * the process can abort rather than discard the data.
     */
    public static int flushAllNow() {
        if (!DatabaseDefaults.Database.WriteBehind.ENABLED) return 0;
        return WriteBehindRepository.flushAllNow();
    }

    /** Entities still waiting to be written across every repository. */
    public static int pendingWrites() {
        if (!DatabaseDefaults.Database.WriteBehind.ENABLED) return 0;
        return WriteBehindRepository.pendingAll();
    }

    public static void shutdown() {
        WriteBehindRepository.shutdownAll();
        writeBehindCache.clear();
        DatabaseManager.getInstance().shutdown();
    }

    public static CompletableFuture<TransferResult> export(Player player) {
        return DatabaseTransferAPI.export(player);
    }

    public static CompletableFuture<TransferResult> export() {
        return DatabaseTransferAPI.export();
    }

    public static CompletableFuture<TransferResult> export(Player player, String filename) {
        return DatabaseTransferAPI.export(player, filename);
    }

    public static CompletableFuture<TransferResult> export(String filename) {
        return DatabaseTransferAPI.export(filename);
    }

    public static CompletableFuture<TransferResult> importData(Player player, String filename) {
        return DatabaseTransferAPI.importData(player, filename);
    }

    public static CompletableFuture<TransferResult> importData(String filename) {
        return DatabaseTransferAPI.importData(filename);
    }

    public static DatabaseManager getManager() {
        return DatabaseManager.getInstance();
    }
}
