package net.exylia.commons.database.repository;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;
import net.exylia.commons.database.exceptions.RepositoryException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RepositoryImpl<T> implements Repository<T> {

    private final DatabaseAdapter adapter;
    private final Class<T> entityClass;
    private final ExecutorService executor;
    private final DatabaseErrorHandler errorHandler;

    public RepositoryImpl(DatabaseAdapter adapter, Class<T> entityClass, ExecutorService executor, DatabaseErrorHandler errorHandler) {
        this.adapter = adapter;
        this.entityClass = entityClass;
        this.executor = executor;
        this.errorHandler = errorHandler;
    }

    @Override
    public void save(T entity) {
        try {
            adapter.save(entity);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("save", entityClass.getSimpleName(),
                    "Failed to save entity to database", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public void update(T entity) {
        try {
            adapter.update(entity);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("update", entityClass.getSimpleName(),
                    "Failed to update entity in database", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public void delete(T entity) {
        try {
            adapter.delete(entity);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("delete", entityClass.getSimpleName(),
                    "Failed to delete entity from database", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public Optional<T> findById(Object id) {
        try {
            return adapter.findById(entityClass, id);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findById", entityClass.getSimpleName(),
                    String.format("Failed to find entity by ID: %s", id), e);
            errorHandler.handleError(repoException);
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return adapter.findAll(entityClass);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findAll", entityClass.getSimpleName(),
                    "Failed to retrieve all entities from database", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public List<T> findBy(String field, Object value) {
        try {
            return adapter.findBy(entityClass, field, value);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findBy", entityClass.getSimpleName(),
                    String.format("Failed to find entities by field '%s' with value: %s", field, value), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public boolean exists(Object id) {
        try {
            return findById(id).isPresent();
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("exists", entityClass.getSimpleName(),
                    String.format("Failed to check entity existence for ID: %s", id), e);
            errorHandler.handleError(repoException);
            return false;
        }
    }

    @Override
    public long count() {
        try {
            return findAll().size();
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("count", entityClass.getSimpleName(),
                    "Failed to count entities in database", e);
            errorHandler.handleError(repoException);
            return 0;
        }
    }

    @Override
    public CompletableFuture<Void> saveAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                save(entity);
            } catch (Exception e) {
                throw new RepositoryException("saveAsync", entityClass.getSimpleName(),
                        "Failed in async save operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                update(entity);
            } catch (Exception e) {
                throw new RepositoryException("updateAsync", entityClass.getSimpleName(),
                        "Failed in async update operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                delete(entity);
            } catch (Exception e) {
                throw new RepositoryException("deleteAsync", entityClass.getSimpleName(),
                        "Failed in async delete operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findById(id);
            } catch (Exception e) {
                RepositoryException repoException = new RepositoryException("findByIdAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findById for ID: %s", id), e);
                errorHandler.handleError(repoException);
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
                throw new RepositoryException("findAllAsync", entityClass.getSimpleName(),
                        "Failed in async findAll operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findByAsync(String field, Object value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findBy(field, value);
            } catch (Exception e) {
                throw new RepositoryException("findByAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findBy for field '%s' with value: %s", field, value), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> existsAsync(Object id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exists(id);
            } catch (Exception e) {
                RepositoryException repoException = new RepositoryException("existsAsync", entityClass.getSimpleName(),
                        String.format("Failed in async exists check for ID: %s", id), e);
                errorHandler.handleError(repoException);
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
                RepositoryException repoException = new RepositoryException("countAsync", entityClass.getSimpleName(),
                        "Failed in async count operation", e);
                errorHandler.handleError(repoException);
                return 0L;
            }
        }, executor);
    }

    @Override
    public List<T> query(String query, Object... params) {
        try {
            return adapter.executeQuery(entityClass, query, params);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("query", entityClass.getSimpleName(),
                    String.format("Failed to execute custom query: %s", query), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public CompletableFuture<List<T>> queryAsync(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(query, params);
            } catch (Exception e) {
                throw new RepositoryException("queryAsync", entityClass.getSimpleName(),
                        String.format("Failed in async custom query: %s", query), e);
            }
        }, executor);
    }

    @Override
    public void saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (T entity : entities) {
            try {
                save(entity);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errorHandler.logWarning("saveAll", entityClass.getSimpleName(),
                        String.format("Failed to save entity %d of %d: %s",
                                successCount + failureCount, entities.size(), e.getMessage()));
            }
        }

        if (failureCount > 0) {
            String message = String.format("saveAll completed with %d successes and %d failures out of %d total entities",
                    successCount, failureCount, entities.size());
            errorHandler.logWarning("saveAll", entityClass.getSimpleName(), message);
        }
    }

    @Override
    public void deleteAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (T entity : entities) {
            try {
                delete(entity);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errorHandler.logWarning("deleteAll", entityClass.getSimpleName(),
                        String.format("Failed to delete entity %d of %d: %s",
                                successCount + failureCount, entities.size(), e.getMessage()));
            }
        }

        if (failureCount > 0) {
            String message = String.format("deleteAll completed with %d successes and %d failures out of %d total entities",
                    successCount, failureCount, entities.size());
            errorHandler.logWarning("deleteAll", entityClass.getSimpleName(), message);
        }
    }

    @Override
    public void saveOrUpdate(T entity) {
        try {
            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = getPrimaryKeyValue(entity, primaryKey);

            if (primaryKeyValue != null && exists(primaryKeyValue)) {
                update(entity);
            } else {
                save(entity);
            }
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("saveOrUpdate", entityClass.getSimpleName(),
                    "Failed to perform saveOrUpdate operation", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public void saveOrUpdateAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        try {
            adapter.saveOrUpdateAll(entities);
        } catch (Exception e) {
            // If adapter doesn't support bulk operations, do it individually
            errorHandler.logWarning("saveOrUpdateAll", entityClass.getSimpleName(),
                    "Adapter doesn't support bulk saveOrUpdate, executing individually: " + e.getMessage());

            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    saveOrUpdate(entity);
                    successCount++;
                } catch (Exception entityException) {
                    failureCount++;
                    errorHandler.logWarning("saveOrUpdateAll", entityClass.getSimpleName(),
                            String.format("Failed to saveOrUpdate entity %d of %d: %s",
                                    successCount + failureCount, entities.size(), entityException.getMessage()));
                }
            }

            if (failureCount > 0) {
                String message = String.format("saveOrUpdateAll completed with %d successes and %d failures out of %d total entities",
                        successCount, failureCount, entities.size());
                errorHandler.logWarning("saveOrUpdateAll", entityClass.getSimpleName(), message);
            }
        }
    }

    @Override
    public CompletableFuture<Void> saveOrUpdateAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveOrUpdate(entity);
            } catch (Exception e) {
                throw new RepositoryException("saveOrUpdateAsync", entityClass.getSimpleName(),
                        "Failed in async saveOrUpdate operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveOrUpdateAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveOrUpdateAll(entities);
            } catch (Exception e) {
                throw new RepositoryException("saveOrUpdateAllAsync", entityClass.getSimpleName(),
                        "Failed in async saveOrUpdateAll operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> saveAll(entities), executor);
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> deleteAll(entities), executor);
    }

    // Helper method to get primary key value from entity
    private Object getPrimaryKeyValue(Object entity, String primaryKeyField) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                if (fieldName.equals(primaryKeyField)) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
        }
        return null;
    }

    // Helper method to get primary key field name
    private String getPrimaryKeyField(Class<?> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    return column.name().isEmpty() ? field.getName() : column.name();
                }
            }
        }
        return "id"; // fallback
    }
}