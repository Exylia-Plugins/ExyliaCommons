package net.exylia.commons.database.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<T> {

    // Operaciones síncronas
    void save(T entity);
    void update(T entity);
    void delete(T entity);
    Optional<T> findById(Object id);
    List<T> findAll();
    List<T> findBy(String field, Object value);
    boolean exists(Object id);
    long count();

    // Operaciones asíncronas
    CompletableFuture<Void> saveAsync(T entity);
    CompletableFuture<Void> updateAsync(T entity);
    CompletableFuture<Void> deleteAsync(T entity);
    CompletableFuture<Optional<T>> findByIdAsync(Object id);
    CompletableFuture<List<T>> findAllAsync();
    CompletableFuture<List<T>> findByAsync(String field, Object value);
    CompletableFuture<Boolean> existsAsync(Object id);
    CompletableFuture<Long> countAsync();

    // Consultas personalizadas
    List<T> query(String query, Object... params);
    CompletableFuture<List<T>> queryAsync(String query, Object... params);

    // Operaciones por lotes
    void saveAll(List<T> entities);
    void saveOrUpdate(T entity);
    CompletableFuture<Void> saveOrUpdateAsync(T entity);
    void deleteAll(List<T> entities);
    CompletableFuture<Void> saveAllAsync(List<T> entities);
    CompletableFuture<Void> deleteAllAsync(List<T> entities);
}