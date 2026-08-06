package net.exylia.commons.v2.database.repository;

import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<T extends Entity> {

    CompletableFuture<Optional<T>> findByIdAsync(Object id);

    Optional<T> findById(Object id);

    CompletableFuture<List<T>> findAllAsync();

    List<T> findAll();

    CompletableFuture<Optional<T>> findByAsync(String fieldName, Object value);

    Optional<T> findBy(String fieldName, Object value);

    CompletableFuture<List<T>> findAllByAsync(String fieldName, Object value);

    List<T> findAllBy(String fieldName, Object value);

    CompletableFuture<Long> countAsync();

    long count();

    CompletableFuture<Long> countByAsync(String fieldName, Object value);

    long countBy(String fieldName, Object value);

    CompletableFuture<Boolean> existsAsync(Object id);

    boolean exists(Object id);

    CompletableFuture<Void> saveAsync(T entity);

    void save(T entity);

    CompletableFuture<Void> saveAllAsync(List<T> entities);

    void saveAll(List<T> entities);

    CompletableFuture<Void> deleteAsync(T entity);

    void delete(T entity);

    CompletableFuture<Void> deleteAllAsync(List<T> entities);

    void deleteAll(List<T> entities);

    /**
     * Wipes the entire table/collection in a single bulk operation instead of
     * loading and deleting entities one by one. Prefer this over paginating
     * + {@link #deleteAll(List)} for "clear everything" operations — it stays
     * O(1) round-trips regardless of table size. Returns the number of rows
     * removed.
     */
    CompletableFuture<Integer> truncateAsync();

    int truncate();

    CompletableFuture<List<T>> findAllOrderedByAsync(String fieldName, boolean ascending, int limit);

    List<T> findAllOrderedBy(String fieldName, boolean ascending, int limit);

    CompletableFuture<List<T>> findAllByOrderedByAsync(String whereField, Object whereValue, String orderField, boolean ascending, int limit);

    List<T> findAllByOrderedBy(String whereField, Object whereValue, String orderField, boolean ascending, int limit);

    CompletableFuture<List<T>> findAllPagedAsync(int page, int pageSize);

    List<T> findAllPaged(int page, int pageSize);

    CompletableFuture<List<T>> findAllPagedOrderedByAsync(String fieldName, boolean ascending, int page, int pageSize);

    List<T> findAllPagedOrderedBy(String fieldName, boolean ascending, int page, int pageSize);

    void putToCache(T entity);

    default CompletableFuture<Void> flushEntity(Object id) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Void> flushEntitiesBy(String fieldName, Object value) {
        return CompletableFuture.completedFuture(null);
    }

    void invalidateCache();

    void invalidateCache(Object id);

    /**
     * Drops only the local (per-JVM) cached copy of this entity, forcing the next
     * {@link #findById(Object)} to go through to the shared L2 store (Redis) instead
     * of trusting a possibly-stale local snapshot. Does NOT delete from Redis and does
     * NOT publish a cross-server invalidation.
     * <p>
     * Use this on player join / server switch to guarantee the freshest cross-server
     * state is read, even if a previous PlayerQuitEvent invalidation message from
     * another server hasn't been processed yet (pub/sub is best-effort/async and has
     * no ordering guarantee relative to the player's own reconnect).
     */
    default void invalidateCacheLocal(Object id) {
        invalidateCache(id);
    }

    CacheStats getCacheStats();

    Class<T> getEntityClass();
}
