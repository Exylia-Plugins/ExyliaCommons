package net.exylia.commons.v2.yaml.repository;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.yaml.adapter.YamlStorageAdapter;
import net.exylia.commons.v2.yaml.exception.YamlStorageException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
public class YamlRepositoryImpl<T extends Entity> implements YamlRepository<T> {

    private final Class<T> entityClass;
    private final EntityMetadata metadata;
    private final YamlStorageAdapter adapter;
    private final CacheStrategy<CacheKey, Object> cache;

    public YamlRepositoryImpl(Class<T> entityClass, EntityMetadata metadata, YamlStorageAdapter adapter, CacheStrategy<CacheKey, Object> cache) {
        this.entityClass = entityClass;
        this.metadata = metadata;
        this.adapter = adapter;
        this.cache = cache;
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(Object id) {
        return Tasks.dbValue(() -> findById(id));
    }

    @Override
    public Optional<T> findById(Object id) {
        CacheKey key = CacheKey.of(entityClass, id);
        Object cached = cache.get(key, k -> {
            try {
                return adapter.load(id, entityClass, metadata).orElse(null);
            } catch (Exception e) {
                throw new YamlStorageException("Error loading entity by id: " + id, e);
            }
        });
        return Optional.ofNullable((T) cached);
    }

    @Override
    public CompletableFuture<List<T>> findAllAsync() {
        return Tasks.dbValue(this::findAll);
    }

    @Override
    public List<T> findAll() {
        try {
            return adapter.loadAll(entityClass, metadata);
        } catch (Exception e) {
            throw new YamlStorageException("Error finding all entities", e);
        }
    }

    @Override
    public CompletableFuture<Optional<T>> findByAsync(String fieldName, Object value) {
        return Tasks.dbValue(() -> findBy(fieldName, value));
    }

    @Override
    public Optional<T> findBy(String fieldName, Object value) {
        CacheKey key = CacheKey.of(entityClass.getSimpleName() + ":" + fieldName, value);
        Object cached = cache.get(key, k -> {
            try {
                List<T> all = adapter.loadAll(entityClass, metadata);
                return all.stream()
                    .filter(entity -> {
                        try {
                            Object fieldValue = metadata.getField(fieldName).getValue(entity);
                            return value.equals(fieldValue);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                throw new YamlStorageException("Error finding entity by " + fieldName + ": " + value, e);
            }
        });
        return Optional.ofNullable((T) cached);
    }

    @Override
    public CompletableFuture<List<T>> findAllByAsync(String fieldName, Object value) {
        return Tasks.dbValue(() -> findAllBy(fieldName, value));
    }

    @Override
    public List<T> findAllBy(String fieldName, Object value) {
        CacheKey key = CacheKey.of(entityClass.getSimpleName() + ":list:" + fieldName, value);
        Object cached = cache.get(key, k -> {
            try {
                List<T> all = adapter.loadAll(entityClass, metadata);
                List<T> results = all.stream()
                    .filter(entity -> {
                        try {
                            Object fieldValue = metadata.getField(fieldName).getValue(entity);
                            return value.equals(fieldValue);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
                return results.isEmpty() ? null : results;
            } catch (Exception e) {
                throw new YamlStorageException("Error finding entities by " + fieldName + ": " + value, e);
            }
        });
        return cached != null ? (List<T>) cached : new ArrayList<>();
    }

    @Override
    public CompletableFuture<Long> countAsync() {
        return Tasks.dbValue(this::count);
    }

    @Override
    public long count() {
        return findAll().size();
    }

    @Override
    public CompletableFuture<Long> countByAsync(String fieldName, Object value) {
        return Tasks.dbValue(() -> countBy(fieldName, value));
    }

    @Override
    public long countBy(String fieldName, Object value) {
        return findAll().stream()
            .filter(entity -> {
                try {
                    Object fieldValue = metadata.getField(fieldName).getValue(entity);
                    return value.equals(fieldValue);
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
    }

    @Override
    public CompletableFuture<Boolean> existsAsync(Object id) {
        return Tasks.dbValue(() -> exists(id));
    }

    @Override
    public boolean exists(Object id) {
        return findById(id).isPresent();
    }

    @Override
    public CompletableFuture<Void> saveAsync(T entity) {
        return Tasks.dbRun(() -> save(entity));
    }

    @Override
    public void save(T entity) {
        try {
            entity.updateTimestamp();
            adapter.save(entity, metadata);
            invalidateCache();
        } catch (Exception e) {
            throw new YamlStorageException("Error saving entity", e);
        }
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(List<T> entities) {
        return Tasks.dbRun(() -> saveAll(entities));
    }

    @Override
    public void saveAll(List<T> entities) {
        entities.forEach(this::save);
    }

    /**
     * No distinct bulk path here: YAML storage writes a file per entity and keeps
     * no batch in flight, so there is nothing to hold off the heap. Delegates to
     * {@link #saveAll(List)} to keep the contract.
     */
    @Override
    public void bulkLoad(List<T> entities) {
        saveAll(entities);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return Tasks.dbRun(() -> delete(entity));
    }

    @Override
    public void delete(T entity) {
        try {
            adapter.delete(entity.getId(), metadata);
            invalidateCache(entity.getId());
        } catch (Exception e) {
            throw new YamlStorageException("Error deleting entity", e);
        }
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        return Tasks.dbRun(() -> deleteAll(entities));
    }

    @Override
    public void deleteAll(List<T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public CompletableFuture<Integer> truncateAsync() {
        return Tasks.dbValue(this::truncate);
    }

    @Override
    public int truncate() {
        int count = adapter.truncate(metadata);
        invalidateCache();
        return count;
    }

    @Override
    public CompletableFuture<Integer> deleteWhereLessThanAsync(String field, Object value, int limit) {
        return Tasks.dbValue(() -> deleteWhereLessThan(field, value, limit));
    }

    /**
     * Unlike the database repositories, this backend stores one file per entity
     * and has no server-side bulk delete, so the candidates must be read to be
     * compared. The point of the bulk primitive — not materialising rows — cannot
     * be honoured here; this exists so the contract holds. Keep large,
     * high-churn tables (match history, event logs) on a database repository.
     */
    @Override
    public int deleteWhereLessThan(String field, Object value, int limit) {
        List<T> victims = new ArrayList<>();
        for (T entity : findAll()) {
            if (victims.size() >= limit) break;
            if (isLessThan(metadata.getField(field).getValue(entity), value)) victims.add(entity);
        }
        victims.forEach(this::delete);
        return victims.size();
    }

    @Override
    public CompletableFuture<Integer> deleteBoundedAsync(int limit) {
        return Tasks.dbValue(() -> deleteBounded(limit));
    }

    @Override
    public int deleteBounded(int limit) {
        List<T> all = findAll();
        List<T> victims = all.size() > limit ? all.subList(0, limit) : all;
        int removed = victims.size();
        new ArrayList<>(victims).forEach(this::delete);
        return removed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isLessThan(Object candidate, Object bound) {
        if (candidate == null || bound == null) return false;
        if (candidate instanceof Number a && bound instanceof Number b) {
            return a.doubleValue() < b.doubleValue();
        }
        if (candidate instanceof Comparable a && candidate.getClass() == bound.getClass()) {
            return a.compareTo(bound) < 0;
        }
        return false;
    }

    @Override
    public CompletableFuture<List<T>> findAllOrderedByAsync(String fieldName, boolean ascending, int limit) {
        return Tasks.dbValue(() -> findAllOrderedBy(fieldName, ascending, limit));
    }

    @Override
    public List<T> findAllOrderedBy(String fieldName, boolean ascending, int limit) {
        List<T> all = findAll();

        Comparator<T> comparator = (e1, e2) -> {
            try {
                Object v1 = metadata.getField(fieldName).getValue(e1);
                Object v2 = metadata.getField(fieldName).getValue(e2);

                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    return ((Comparable) v1).compareTo(v2);
                }
                return 0;
            } catch (Exception e) {
                return 0;
            }
        };

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return all.stream()
            .sorted(comparator)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<T>> findAllByOrderedByAsync(String whereField, Object whereValue, String orderField, boolean ascending, int limit) {
        return Tasks.dbValue(() -> findAllByOrderedBy(whereField, whereValue, orderField, ascending, limit));
    }

    @Override
    public List<T> findAllByOrderedBy(String whereField, Object whereValue, String orderField, boolean ascending, int limit) {
        List<T> filtered = findAllBy(whereField, whereValue);

        Comparator<T> comparator = (e1, e2) -> {
            try {
                Object v1 = metadata.getField(orderField) != null
                        ? metadata.getField(orderField).getValue(e1)
                        : null;
                Object v2 = metadata.getField(orderField) != null
                        ? metadata.getField(orderField).getValue(e2)
                        : null;
                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    return ((Comparable) v1).compareTo(v2);
                }
                return 0;
            } catch (Exception e) {
                return 0;
            }
        };

        if (!ascending) comparator = comparator.reversed();

        return filtered.stream()
                .sorted(comparator)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedAsync(int page, int pageSize) {
        return Tasks.dbValue(() -> findAllPaged(page, pageSize));
    }

    @Override
    public List<T> findAllPaged(int page, int pageSize) {
        List<T> all = findAll();
        int skip = page * pageSize;

        return all.stream()
            .skip(skip)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedOrderedByAsync(String fieldName, boolean ascending, int page, int pageSize) {
        return Tasks.dbValue(() -> findAllPagedOrderedBy(fieldName, ascending, page, pageSize));
    }

    @Override
    public List<T> findAllPagedOrderedBy(String fieldName, boolean ascending, int page, int pageSize) {
        List<T> ordered = findAllOrderedBy(fieldName, ascending, Integer.MAX_VALUE);
        int skip = page * pageSize;

        return ordered.stream()
            .skip(skip)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    @Override
    public void putToCache(T entity) {
        if (entity == null || entity.getId() == null) return;
        cache.put(CacheKey.of(entityClass, entity.getId()), entity);
    }

    @Override
    public void invalidateCache() {
        cache.invalidateAll();
    }

    @Override
    public void invalidateCache(Object id) {
        cache.invalidate(CacheKey.of(entityClass, id));
    }

    @Override
    public CacheStats getCacheStats() {
        return cache.getStats();
    }
}
