package net.exylia.commons.database.repository;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.utils.DebugUtils;

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
            DebugUtils.logError("Error guardando entidad " + entityClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException("Error guardando entidad: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(T entity) {
        try {
            adapter.update(entity);
        } catch (Exception e) {
            DebugUtils.logError("Error actualizando entidad " + entityClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException("Error actualizando entidad: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(T entity) {
        try {
            adapter.delete(entity);
        } catch (Exception e) {
            DebugUtils.logError("Error eliminando entidad " + entityClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException("Error eliminando entidad: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<T> findById(Object id) {
        try {
            return adapter.findById(entityClass, id);
        } catch (Exception e) {
            DebugUtils.logError("Error buscando entidad " + entityClass.getSimpleName() + " por ID " + id + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return adapter.findAll(entityClass);
        } catch (Exception e) {
            DebugUtils.logError("Error obteniendo todas las entidades " + entityClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException("Error obteniendo todas las entidades: " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> findBy(String field, Object value) {
        try {
            return adapter.findBy(entityClass, field, value);
        } catch (Exception e) {
            DebugUtils.logError("Error buscando entidades " + entityClass.getSimpleName() + " por campo " + field + ": " + e.getMessage());
            throw new RuntimeException("Error buscando entidades por campo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(Object id) {
        try {
            return findById(id).isPresent();
        } catch (Exception e) {
            DebugUtils.logError("Error verificando existencia de entidad " + entityClass.getSimpleName() + " con ID " + id + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        try {
            return findAll().size();
        } catch (Exception e) {
            DebugUtils.logError("Error contando entidades " + entityClass.getSimpleName() + ": " + e.getMessage());
            return 0;
        }
    }

    @Override
    public CompletableFuture<Void> saveAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                save(entity);
            } catch (Exception e) {
                throw new RuntimeException("Error en saveAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                update(entity);
            } catch (Exception e) {
                throw new RuntimeException("Error en updateAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                delete(entity);
            } catch (Exception e) {
                throw new RuntimeException("Error en deleteAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findById(id);
            } catch (Exception e) {
                DebugUtils.logError("Error en findByIdAsync para ID " + id + ": " + e.getMessage());
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAll();
            } catch (Exception e) {
                throw new RuntimeException("Error en findAllAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findByAsync(String field, Object value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findBy(field, value);
            } catch (Exception e) {
                throw new RuntimeException("Error en findByAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> existsAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exists(id);
            } catch (Exception e) {
                DebugUtils.logError("Error en existsAsync para ID " + id + ": " + e.getMessage());
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Long> countAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return count();
            } catch (Exception e) {
                DebugUtils.logError("Error en countAsync: " + e.getMessage());
                return 0L;
            }
        }, executor);
    }

    @Override
    public List<T> query(String query, Object... params) {
        try {
            return adapter.executeQuery(entityClass, query, params);
        } catch (Exception e) {
            DebugUtils.logError("Error ejecutando consulta personalizada: " + e.getMessage());
            throw new RuntimeException("Error ejecutando consulta personalizada: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<List<T>> queryAsync(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(query, params);
            } catch (Exception e) {
                throw new RuntimeException("Error en queryAsync: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public void saveAll(List<T> entities) {
        for (T entity : entities) {
            try {
                save(entity);
            } catch (Exception e) {
                DebugUtils.logError("Error guardando entidad en saveAll: " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteAll(List<T> entities) {
        for (T entity : entities) {
            try {
                delete(entity);
            } catch (Exception e) {
                DebugUtils.logError("Error eliminando entidad en deleteAll: " + e.getMessage());
            }
        }
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