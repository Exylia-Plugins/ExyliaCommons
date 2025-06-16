package net.exylia.commons.database.repository;

import net.exylia.commons.database.adapters.DatabaseAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RepositoryImpl<T> implements Repository<T> {

    private final DatabaseAdapter adapter;
    private final Class<T> entityClass;
    private final ExecutorService executor;

    public RepositoryImpl(DatabaseAdapter adapter, Class<T> entityClass, ExecutorService executor) {
        this.adapter = adapter;
        this.entityClass = entityClass;
        this.executor = executor;
    }

    @Override
    public void save(T entity) {
        try {
            adapter.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando entidad " + e.getMessage(), e);
        }
    }

    @Override
    public void update(T entity) {
        try {
            adapter.update(entity);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando entidad", e);
        }
    }

    @Override
    public void delete(T entity) {
        try {
            adapter.delete(entity);
        } catch (Exception e) {
            throw new RuntimeException("Error eliminando entidad", e);
        }
    }

    @Override
    public Optional<T> findById(Object id) {
        try {
            return adapter.findById(entityClass, id);
        } catch (Exception e) {
            throw new RuntimeException("Error buscando entidad por ID", e);
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return adapter.findAll(entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo todas las entidades", e);
        }
    }

    @Override
    public List<T> findBy(String field, Object value) {
        try {
            return adapter.findBy(entityClass, field, value);
        } catch (Exception e) {
            throw new RuntimeException("Error buscando entidades por campo", e);
        }
    }

    @Override
    public boolean exists(Object id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        return findAll().size();
    }

    // Métodos asíncronos
    @Override
    public CompletableFuture<Void> saveAsync(T entity) {
        return CompletableFuture.runAsync(() -> save(entity), executor);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> update(entity), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(() -> delete(entity), executor);
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> findById(id), executor);
    }

    @Override
    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(this::findAll, executor);
    }

    @Override
    public CompletableFuture<List<T>> findByAsync(String field, Object value) {
        return CompletableFuture.supplyAsync(() -> findBy(field, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> existsAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> exists(id), executor);
    }

    @Override
    public CompletableFuture<Long> countAsync() {
        return CompletableFuture.supplyAsync(this::count, executor);
    }

    @Override
    public List<T> query(String query, Object... params) {
        try {
            return adapter.executeQuery(entityClass, query, params);
        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando consulta personalizada", e);
        }
    }

    @Override
    public CompletableFuture<List<T>> queryAsync(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> query(query, params), executor);
    }

    @Override
    public void saveAll(List<T> entities) {
        entities.forEach(this::save);
    }

    @Override
    public void deleteAll(List<T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> saveAll(entities), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> deleteAll(entities), executor);
    }
}