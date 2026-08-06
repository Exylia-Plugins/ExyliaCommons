package net.exylia.commons.v2.database.repository;

import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.api.Tasks;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class WriteBehindRepository<T extends Entity> implements Repository<T> {

    private static final List<WriteBehindRepository<?>> INSTANCES = new CopyOnWriteArrayList<>();

    private final Repository<T> delegate;
    private final ConcurrentHashMap<Object, T> dirtyEntities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, T> pendingDeletes = new ConcurrentHashMap<>();
    private volatile boolean shutdown = false;
    private final String entityName;

    public WriteBehindRepository(Repository<T> delegate, long flushIntervalSeconds) {
        this.delegate = delegate;
        this.entityName = delegate.getEntityClass().getSimpleName();
        long ticks = flushIntervalSeconds * 20L;
        TaskAPI.syncTimer(this::scheduledFlush, ticks, ticks);
        INSTANCES.add(this);
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Registered for " + entityName + " (flush every " + flushIntervalSeconds + "s)");
    }

    private void scheduledFlush() {
        if (shutdown || (dirtyEntities.isEmpty() && pendingDeletes.isEmpty())) return;
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Scheduled flush triggered for " + entityName + " — dirty=" + dirtyEntities.size() + " deletes=" + pendingDeletes.size());
        Tasks.dbRun(this::flushNow);
    }

    void flushNow() {
        int saved = 0;
        int deleted = 0;

        if (!dirtyEntities.isEmpty()) {
            List<T> toSave = new ArrayList<>();
            dirtyEntities.forEach((id, entity) -> {
                if (dirtyEntities.remove(id, entity)) toSave.add(entity);
            });
            if (!toSave.isEmpty()) {
                delegate.saveAll(toSave);
                saved = toSave.size();
            }
        }

        if (!pendingDeletes.isEmpty()) {
            List<T> toDelete = new ArrayList<>();
            pendingDeletes.forEach((id, entity) -> {
                if (pendingDeletes.remove(id, entity)) toDelete.add(entity);
            });
            if (!toDelete.isEmpty()) {
                delegate.deleteAll(toDelete);
                deleted = toDelete.size();
            }
        }

        if (saved > 0 || deleted > 0) {
            DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Flush complete for " + entityName + " — saved=" + saved + " deleted=" + deleted);
        }
    }

    public void flush() {
        if (!dirtyEntities.isEmpty() || !pendingDeletes.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Manual flush requested for " + entityName + " — dirty=" + dirtyEntities.size() + " deletes=" + pendingDeletes.size());
            Tasks.dbRun(this::flushNow);
        }
    }

    private void shutdownFlush() {
        this.shutdown = true;
        INSTANCES.remove(this);
        int dirty = dirtyEntities.size();
        int deletes = pendingDeletes.size();
        if (dirty > 0 || deletes > 0) {
            DebugAPI.logLibWarn(DebugCategory.DATABASE, "[WriteBehind] Shutdown flush for " + entityName + " — saving " + dirty + " dirty, " + deletes + " pending deletes");
            flushNow();
            DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Shutdown flush complete for " + entityName);
        } else {
            DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Shutdown for " + entityName + " — nothing to flush");
        }
    }

    public CompletableFuture<Void> flushEntity(Object id) {
        T entity = dirtyEntities.remove(id);
        T toDelete = pendingDeletes.remove(id);
        if (entity == null && toDelete == null) return CompletableFuture.completedFuture(null);
        final T toSave = entity;
        final T toDel = toDelete;
        return Tasks.dbRun(() -> {
            if (toSave != null) delegate.save(toSave);
            if (toDel != null) delegate.delete(toDel);
        });
    }

    @Override
    public CompletableFuture<Void> flushEntitiesBy(String fieldName, Object value) {
        List<T> toSave = new ArrayList<>();
        List<T> toDelete = new ArrayList<>();

        for (Map.Entry<Object, T> entry : dirtyEntities.entrySet()) {
            if (matchesField(entry.getValue(), fieldName, value)) {
                T removed = dirtyEntities.remove(entry.getKey());
                if (removed != null) toSave.add(removed);
            }
        }
        for (Map.Entry<Object, T> entry : pendingDeletes.entrySet()) {
            if (matchesField(entry.getValue(), fieldName, value)) {
                T removed = pendingDeletes.remove(entry.getKey());
                if (removed != null) toDelete.add(removed);
            }
        }

        if (toSave.isEmpty() && toDelete.isEmpty()) return CompletableFuture.completedFuture(null);

        final List<T> finalToSave = toSave;
        final List<T> finalToDelete = toDelete;
        return Tasks.dbRun(() -> {
            if (!finalToSave.isEmpty()) delegate.saveAll(finalToSave);
            if (!finalToDelete.isEmpty()) delegate.deleteAll(finalToDelete);
        });
    }

    public static void shutdownAll() {
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Shutting down " + INSTANCES.size() + " repositories");
        new ArrayList<>(INSTANCES).forEach(WriteBehindRepository::shutdownFlush);
    }

    // --- Writes (buffered) ---

    @Override
    public void putToCache(T entity) {
        delegate.putToCache(entity);
    }

    @Override
    public void save(T entity) {
        if (shutdown) {
            DebugAPI.logLibWarn(DebugCategory.DATABASE, "[WriteBehind] Save called after shutdown for " + entityName + " [" + entity.getId() + "] — saving directly");
            Tasks.dbRun(() -> delegate.save(entity));
            return;
        }
        Object id = entity.getId();
        pendingDeletes.remove(id);
        dirtyEntities.put(id, entity);
        delegate.putToCache(entity);
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Queued save for " + entityName + " [" + id + "] — dirty=" + dirtyEntities.size());
    }

    @Override
    public CompletableFuture<Void> saveAsync(T entity) {
        save(entity);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void saveAll(List<T> entities) {
        entities.forEach(this::save);
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(List<T> entities) {
        saveAll(entities);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void delete(T entity) {
        if (shutdown) {
            DebugAPI.logLibWarn(DebugCategory.DATABASE, "[WriteBehind] Delete called after shutdown for " + entityName + " [" + entity.getId() + "] — deleting directly");
            Tasks.dbRun(() -> delegate.delete(entity));
            return;
        }
        Object id = entity.getId();
        boolean wasDirty = dirtyEntities.remove(id) != null;
        pendingDeletes.put(id, entity);
        delegate.invalidateCache(id);
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Queued delete for " + entityName + " [" + id + "]" + (wasDirty ? " (removed from dirty)" : "") + " — pending=" + pendingDeletes.size());
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        delete(entity);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void deleteAll(List<T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        deleteAll(entities);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Integer> truncateAsync() {
        return Tasks.dbValue(this::truncate);
    }

    @Override
    public int truncate() {
        // Everything is being wiped at the storage layer, so any queued
        // writes/deletes for this entity are now moot — drop them first so a
        // subsequent scheduled flush doesn't resurrect rows that truncate()
        // just removed.
        dirtyEntities.clear();
        pendingDeletes.clear();
        int count = delegate.truncate();
        DebugAPI.logLibDebug(DebugCategory.DATABASE, "[WriteBehind] Truncated " + entityName + " (" + count + " rows) — dropped any pending writes/deletes");
        return count;
    }

    // --- Reads (read-your-writes) ---

    @Override
    public Optional<T> findById(Object id) {
        if (pendingDeletes.containsKey(id)) return Optional.empty();
        T dirty = dirtyEntities.get(id);
        if (dirty != null) return Optional.of(dirty);
        return delegate.findById(id);
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(Object id) {
        if (pendingDeletes.containsKey(id)) return CompletableFuture.completedFuture(Optional.empty());
        T dirty = dirtyEntities.get(id);
        if (dirty != null) return CompletableFuture.completedFuture(Optional.of(dirty));
        return delegate.findByIdAsync(id);
    }

    @Override
    public boolean exists(Object id) {
        if (dirtyEntities.containsKey(id)) return true;
        if (pendingDeletes.containsKey(id)) return false;
        return delegate.exists(id);
    }

    @Override
    public CompletableFuture<Boolean> existsAsync(Object id) {
        if (dirtyEntities.containsKey(id)) return CompletableFuture.completedFuture(true);
        if (pendingDeletes.containsKey(id)) return CompletableFuture.completedFuture(false);
        return delegate.existsAsync(id);
    }

    @Override
    public List<T> findAll() {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findAll();
        Map<Object, T> merged = new LinkedHashMap<>();
        delegate.findAll().forEach(e -> merged.put(e.getId(), e));
        pendingDeletes.keySet().forEach(merged::remove);
        dirtyEntities.forEach(merged::put);
        return new ArrayList<>(merged.values());
    }

    @Override
    public CompletableFuture<List<T>> findAllAsync() {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findAllAsync();
        return delegate.findAllAsync().thenApply(list -> {
            Map<Object, T> merged = new LinkedHashMap<>();
            list.forEach(e -> merged.put(e.getId(), e));
            pendingDeletes.keySet().forEach(merged::remove);
            dirtyEntities.forEach(merged::put);
            return new ArrayList<>(merged.values());
        });
    }

    @Override
    public Optional<T> findBy(String fieldName, Object value) {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findBy(fieldName, value);
        Optional<T> fromDirty = dirtyEntities.values().stream()
                .filter(e -> matchesField(e, fieldName, value))
                .findFirst();
        if (fromDirty.isPresent()) return fromDirty;
        return delegate.findBy(fieldName, value)
                .filter(e -> !pendingDeletes.containsKey(e.getId()))
                .filter(e -> !dirtyEntities.containsKey(e.getId()));
    }

    @Override
    public CompletableFuture<Optional<T>> findByAsync(String fieldName, Object value) {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findByAsync(fieldName, value);
        return delegate.findByAsync(fieldName, value).thenApply(opt ->
                opt.filter(e -> !pendingDeletes.containsKey(e.getId()))
                   .filter(e -> !dirtyEntities.containsKey(e.getId()))
        ).thenApply(opt -> {
            if (opt.isPresent()) return opt;
            return dirtyEntities.values().stream()
                    .filter(e -> matchesField(e, fieldName, value))
                    .findFirst();
        });
    }

    @Override
    public List<T> findAllBy(String fieldName, Object value) {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findAllBy(fieldName, value);
        Map<Object, T> merged = new LinkedHashMap<>();
        delegate.findAllBy(fieldName, value).forEach(e -> merged.put(e.getId(), e));
        pendingDeletes.keySet().forEach(merged::remove);
        dirtyEntities.values().stream()
                .filter(e -> matchesField(e, fieldName, value))
                .forEach(e -> merged.put(e.getId(), e));
        return new ArrayList<>(merged.values());
    }

    @Override
    public CompletableFuture<List<T>> findAllByAsync(String fieldName, Object value) {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.findAllByAsync(fieldName, value);
        return delegate.findAllByAsync(fieldName, value).thenApply(list -> {
            Map<Object, T> merged = new LinkedHashMap<>();
            list.forEach(e -> merged.put(e.getId(), e));
            pendingDeletes.keySet().forEach(merged::remove);
            dirtyEntities.values().stream()
                    .filter(e -> matchesField(e, fieldName, value))
                    .forEach(e -> merged.put(e.getId(), e));
            return new ArrayList<>(merged.values());
        });
    }

    @Override
    public long count() {
        if (dirtyEntities.isEmpty() && pendingDeletes.isEmpty()) return delegate.count();
        long base = delegate.count();
        long added = dirtyEntities.keySet().stream().filter(id -> !delegate.exists(id)).count();
        long removed = pendingDeletes.keySet().stream().filter(delegate::exists).count();
        return base + added - removed;
    }

    @Override
    public CompletableFuture<Long> countAsync() {
        return Tasks.dbValue(this::count);
    }

    @Override
    public long countBy(String fieldName, Object value) {
        return findAllBy(fieldName, value).size();
    }

    @Override
    public CompletableFuture<Long> countByAsync(String fieldName, Object value) {
        return findAllByAsync(fieldName, value).thenApply(list -> (long) list.size());
    }

    private boolean matchesField(T entity, String fieldName, Object value) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field == null) return false;
            field.setAccessible(true);
            Object fieldValue = field.get(entity);
            return Objects.equals(fieldValue, value);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public CompletableFuture<List<T>> findAllOrderedByAsync(String f, boolean asc, int limit) { return delegate.findAllOrderedByAsync(f, asc, limit); }
    @Override
    public List<T> findAllOrderedBy(String f, boolean asc, int limit) { return delegate.findAllOrderedBy(f, asc, limit); }
    @Override
    public CompletableFuture<List<T>> findAllByOrderedByAsync(String wf, Object wv, String of, boolean asc, int limit) { return delegate.findAllByOrderedByAsync(wf, wv, of, asc, limit); }
    @Override
    public List<T> findAllByOrderedBy(String wf, Object wv, String of, boolean asc, int limit) { return delegate.findAllByOrderedBy(wf, wv, of, asc, limit); }
    @Override
    public CompletableFuture<List<T>> findAllPagedAsync(int page, int size) { return delegate.findAllPagedAsync(page, size); }
    @Override
    public List<T> findAllPaged(int page, int size) { return delegate.findAllPaged(page, size); }
    @Override
    public CompletableFuture<List<T>> findAllPagedOrderedByAsync(String f, boolean asc, int page, int size) { return delegate.findAllPagedOrderedByAsync(f, asc, page, size); }
    @Override
    public List<T> findAllPagedOrderedBy(String f, boolean asc, int page, int size) { return delegate.findAllPagedOrderedBy(f, asc, page, size); }
    @Override
    public void invalidateCache() { delegate.invalidateCache(); }
    @Override
    public void invalidateCache(Object id) { delegate.invalidateCache(id); }
    @Override
    public void invalidateCacheLocal(Object id) { delegate.invalidateCacheLocal(id); }
    @Override
    public CacheStats getCacheStats() { return delegate.getCacheStats(); }
    @Override
    public Class<T> getEntityClass() { return delegate.getEntityClass(); }
}
