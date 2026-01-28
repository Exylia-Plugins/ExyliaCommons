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
