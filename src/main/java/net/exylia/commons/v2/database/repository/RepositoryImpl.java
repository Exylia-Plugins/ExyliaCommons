package net.exylia.commons.v2.database.repository;

import lombok.Getter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.cache.CacheKey;
import net.exylia.commons.v2.database.cache.CacheStats;
import net.exylia.commons.v2.database.cache.CacheStrategy;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.exception.RepositoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
public class RepositoryImpl<T extends Entity> implements Repository<T> {

    private final Class<T> entityClass;
    private final EntityMetadata metadata;
    private final DatabaseAdapter adapter;
    private final CacheStrategy<CacheKey, Object> cache;

    public RepositoryImpl(Class<T> entityClass, EntityMetadata metadata, DatabaseAdapter adapter, CacheStrategy<CacheKey, Object> cache) {
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
                return adapter.findById(id, entityClass, metadata).orElse(null);
            } catch (Exception e) {
                throw new RepositoryException("Error finding entity by id: " + id, e);
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
        CacheKey key = CacheKey.of(entityClass, "ALL");
        Object cached = cache.get(key, k -> {
            try {
                List<T> results = adapter.findAll(entityClass, metadata);
                return results.isEmpty() ? null : results;
            } catch (Exception e) {
                throw new RepositoryException("Error finding all entities", e);
            }
        });
        return cached != null ? (List<T>) cached : new ArrayList<>();
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
                List<T> results = adapter.findByField(fieldName, value, entityClass, metadata);
                return results.isEmpty() ? null : results.get(0);
            } catch (Exception e) {
                throw new RepositoryException("Error finding entity by " + fieldName + ": " + value, e);
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
                List<T> results = adapter.findByField(fieldName, value, entityClass, metadata);
                return results.isEmpty() ? null : results;
            } catch (Exception e) {
                throw new RepositoryException("Error finding entities by " + fieldName + ": " + value, e);
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
        try {
            return adapter.count(entityClass, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Error counting entities", e);
        }
    }

    @Override
    public CompletableFuture<Long> countByAsync(String fieldName, Object value) {
        return Tasks.dbValue(() -> countBy(fieldName, value));
    }

    @Override
    public long countBy(String fieldName, Object value) {
        try {
            return adapter.countByField(fieldName, value, entityClass, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Error counting by " + fieldName, e);
        }
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

            if (entity.getId() != null && exists(entity.getId())) {
                adapter.update(entity, metadata);
            } else {
                adapter.insert(entity, metadata);
            }

            invalidateCache();
        } catch (Exception e) {
            throw new RepositoryException("Error saving entity", e);
        }
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(List<T> entities) {
        return Tasks.dbRun(() -> saveAll(entities));
    }

    @Override
    public void saveAll(List<T> entities) {
        try {
            for (T entity : entities) {
                entity.updateTimestamp();
            }
            adapter.upsertBatch(entities, metadata);
            invalidateCache();
        } catch (Exception e) {
            throw new RepositoryException("Error saving batch of entities", e);
        }
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return Tasks.dbRun(() -> delete(entity));
    }

    @Override
    public void delete(T entity) {
        try {
            adapter.delete(entity, metadata);
            invalidateCache(entity.getId());
        } catch (Exception e) {
            throw new RepositoryException("Error deleting entity", e);
        }
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        return Tasks.dbRun(() -> deleteAll(entities));
    }

    @Override
    public void deleteAll(List<T> entities) {
        try {
            adapter.deleteBatch(entities, metadata);
            invalidateCache();
        } catch (Exception e) {
            throw new RepositoryException("Error deleting batch of entities", e);
        }
    }

    @Override
    public CompletableFuture<List<T>> findAllOrderedByAsync(String fieldName, boolean ascending, int limit) {
        return Tasks.dbValue(() -> findAllOrderedBy(fieldName, ascending, limit));
    }

    @Override
    public List<T> findAllOrderedBy(String fieldName, boolean ascending, int limit) {
        try {
            List<T> all = adapter.findAllSorted(fieldName, ascending, entityClass, metadata);
            return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
        } catch (Exception e) {
            throw new RepositoryException("Error finding ordered entities", e);
        }
    }

    @Override
    public CompletableFuture<List<T>> findAllByOrderedByAsync(String whereField, Object whereValue, String orderField, boolean ascending, int limit) {
        return Tasks.dbValue(() -> findAllByOrderedBy(whereField, whereValue, orderField, ascending, limit));
    }

    @Override
    public List<T> findAllByOrderedBy(String whereField, Object whereValue, String orderField, boolean ascending, int limit) {
        try {
            return adapter.findByFieldSorted(whereField, whereValue, orderField, ascending, limit, entityClass, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Error finding ordered entities by field", e);
        }
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedAsync(int page, int pageSize) {
        return Tasks.dbValue(() -> findAllPaged(page, pageSize));
    }

    @Override
    public List<T> findAllPaged(int page, int pageSize) {
        try {
            return adapter.findByFieldPaged(null, null, page, pageSize, entityClass, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Error finding paged entities", e);
        }
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedOrderedByAsync(String fieldName, boolean ascending, int page, int pageSize) {
        return Tasks.dbValue(() -> findAllPagedOrderedBy(fieldName, ascending, page, pageSize));
    }

    @Override
    public List<T> findAllPagedOrderedBy(String fieldName, boolean ascending, int page, int pageSize) {
        try {
            return adapter.findAllSortedPaged(fieldName, ascending, page, pageSize, entityClass, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Error finding paged ordered entities", e);
        }
    }

    @Override
    public void invalidateCache() {
        cache.invalidateAll();
    }

    @Override
    public void invalidateCache(Object id) {
        CacheKey key = CacheKey.of(entityClass, id);
        cache.invalidate(key);
    }

    @Override
    public CacheStats getCacheStats() {
        return cache.getStats();
    }
}
