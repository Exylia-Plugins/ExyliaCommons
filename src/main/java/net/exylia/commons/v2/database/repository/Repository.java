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

    CompletableFuture<List<T>> findAllOrderedByAsync(String fieldName, boolean ascending, int limit);

    List<T> findAllOrderedBy(String fieldName, boolean ascending, int limit);

    CompletableFuture<List<T>> findAllByOrderedByAsync(String whereField, Object whereValue, String orderField, boolean ascending, int limit);

    List<T> findAllByOrderedBy(String whereField, Object whereValue, String orderField, boolean ascending, int limit);

    CompletableFuture<List<T>> findAllPagedAsync(int page, int pageSize);

    List<T> findAllPaged(int page, int pageSize);

    CompletableFuture<List<T>> findAllPagedOrderedByAsync(String fieldName, boolean ascending, int page, int pageSize);

    List<T> findAllPagedOrderedBy(String fieldName, boolean ascending, int page, int pageSize);

    void invalidateCache();

    void invalidateCache(Object id);

    CacheStats getCacheStats();

    Class<T> getEntityClass();
}
