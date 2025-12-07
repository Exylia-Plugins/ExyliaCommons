package net.exylia.commons.database.repository;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;
import net.exylia.commons.database.exceptions.RepositoryException;

import java.lang.reflect.Field;
import java.util.*;
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
    public T saveOrUpdate(T entity) {
        try {
            adapter.saveOrUpdateAll(Collections.singletonList(entity));
            return entity;
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("saveOrUpdate", entityClass.getSimpleName(),
                    "Failed to perform saveOrUpdate operation", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public List<T> saveOrUpdateAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        try {
            return adapter.saveOrUpdateAll(entities);
        } catch (Exception e) {
            errorHandler.logInternalWarning("saveOrUpdateAll", entityClass.getSimpleName(),
                    "Adapter doesn't support bulk saveOrUpdate, executing individually: " + e.getMessage());

            int successCount = 0;
            int failureCount = 0;

            for (T entity : entities) {
                try {
                    saveOrUpdate(entity);
                    successCount++;
                } catch (Exception entityException) {
                    failureCount++;
                    errorHandler.logInternalWarning("saveOrUpdateAll", entityClass.getSimpleName(),
                            String.format("Failed to saveOrUpdate entity %d of %d: %s",
                                    successCount + failureCount, entities.size(), entityException.getMessage()));
                }
            }

            if (failureCount > 0) {
                String message = String.format("saveOrUpdateAll completed with %d successes and %d failures out of %d total entities",
                        successCount, failureCount, entities.size());
                errorHandler.logInternalWarning("saveOrUpdateAll", entityClass.getSimpleName(), message);
            }

            return entities;
        }
    }

    @Override
    public boolean delete(T entity) {
        try {
            return adapter.delete(entity);
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
    public CompletableFuture<T> saveOrUpdateAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return saveOrUpdate(entity);
            } catch (Exception e) {
                throw new RepositoryException("saveOrUpdateAsync", entityClass.getSimpleName(),
                        "Failed in async saveOrUpdate operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> saveOrUpdateAllAsync(List<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return saveOrUpdateAll(entities);
            } catch (Exception e) {
                throw new RepositoryException("saveOrUpdateAllAsync", entityClass.getSimpleName(),
                        "Failed in async saveOrUpdateAll operation", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delete(entity);
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
                errorHandler.logInternalWarning("deleteAll", entityClass.getSimpleName(),
                        String.format("Failed to delete entity %d of %d: %s",
                                successCount + failureCount, entities.size(), e.getMessage()));
            }
        }

        if (failureCount > 0) {
            String message = String.format("deleteAll completed with %d successes and %d failures out of %d total entities",
                    successCount, failureCount, entities.size());
            errorHandler.logInternalWarning("deleteAll", entityClass.getSimpleName(), message);
        }
    }

    @Override
    public CompletableFuture<Void> deleteAllAsync(List<T> entities) {
        return CompletableFuture.runAsync(() -> deleteAll(entities), executor);
    }

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
        return "id";  
    }

    @Override
    public List<T> findAllOrderedBy(String field, SortOrder order) {
        try {
            return adapter.findAllOrderedBy(entityClass, field, order);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findAllOrderedBy", entityClass.getSimpleName(),
                    String.format("Failed to find all entities ordered by field '%s' with order %s", field, order), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public List<T> findAllOrderedBy(String field, SortOrder order, int limit) {
        try {
            return adapter.findAllOrderedBy(entityClass, field, order, limit);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findAllOrderedBy", entityClass.getSimpleName(),
                    String.format("Failed to find entities ordered by field '%s' with order %s and limit %d", field, order, limit), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public List<T> findTopN(String field, int n) {
        return findAllOrderedBy(field, SortOrder.DESC, n);
    }

    @Override
    public List<T> findBottomN(String field, int n) {
        return findAllOrderedBy(field, SortOrder.ASC, n);
    }

    @Override
    public CompletableFuture<List<T>> findAllOrderedByAsync(String field, SortOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAllOrderedBy(field, order);
            } catch (Exception e) {
                throw new RepositoryException("findAllOrderedByAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findAllOrderedBy for field '%s' with order %s", field, order), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findAllOrderedByAsync(String field, SortOrder order, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAllOrderedBy(field, order, limit);
            } catch (Exception e) {
                throw new RepositoryException("findAllOrderedByAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findAllOrderedBy for field '%s' with order %s and limit %d", field, order, limit), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findTopNAsync(String field, int n) {
        return findAllOrderedByAsync(field, SortOrder.DESC, n);
    }

    @Override
    public CompletableFuture<List<T>> findBottomNAsync(String field, int n) {
        return findAllOrderedByAsync(field, SortOrder.ASC, n);
    }

    @Override
    public List<T> findAllPaged(int page, int size) {
        try {
            return adapter.findAllPaged(entityClass, page, size);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findAllPaged", entityClass.getSimpleName(),
                    String.format("Failed to find entities with pagination: page %d, size %d", page, size), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public List<T> findAllPagedOrderedBy(String field, SortOrder order, int page, int size) {
        try {
            return adapter.findAllPagedOrderedBy(entityClass, field, order, page, size);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findAllPagedOrderedBy", entityClass.getSimpleName(),
                    String.format("Failed to find entities with pagination and ordering: field '%s', order %s, page %d, size %d", field, order, page, size), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedAsync(int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAllPaged(page, size);
            } catch (Exception e) {
                throw new RepositoryException("findAllPagedAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findAllPaged: page %d, size %d", page, size), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<T>> findAllPagedOrderedByAsync(String field, SortOrder order, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findAllPagedOrderedBy(field, order, page, size);
            } catch (Exception e) {
                throw new RepositoryException("findAllPagedOrderedByAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findAllPagedOrderedBy: field '%s', order %s, page %d, size %d", field, order, page, size), e);
            }
        }, executor);
    }

    @Override
    public long getRankByField(String field, Object value, SortOrder order) {
        try {
            return adapter.getRankByField(entityClass, field, value, order);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("getRankByField", entityClass.getSimpleName(),
                    String.format("Failed to get rank for field '%s' with value %s and order %s", field, value, order), e);
            errorHandler.handleError(repoException);
            return -1;
        }
    }

    @Override
    public Optional<T> getByRank(String field, long rank, SortOrder order) {
        try {
            return adapter.getByRank(entityClass, field, rank, order);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("getByRank", entityClass.getSimpleName(),
                    String.format("Failed to get entity by rank %d for field '%s' with order %s", rank, field, order), e);
            errorHandler.handleError(repoException);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Long> getRankByFieldAsync(String field, Object value, SortOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getRankByField(field, value, order);
            } catch (Exception e) {
                RepositoryException repoException = new RepositoryException("getRankByFieldAsync", entityClass.getSimpleName(),
                        String.format("Failed in async getRankByField for field '%s' with value %s and order %s", field, value, order), e);
                errorHandler.handleError(repoException);
                return -1L;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<T>> getByRankAsync(String field, long rank, SortOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getByRank(field, rank, order);
            } catch (Exception e) {
                RepositoryException repoException = new RepositoryException("getByRankAsync", entityClass.getSimpleName(),
                        String.format("Failed in async getByRank for rank %d, field '%s' with order %s", rank, field, order), e);
                errorHandler.handleError(repoException);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public List<T> getLeaderboard(String field, int limit) {
        return getLeaderboard(field, limit, SortOrder.DESC);
    }

    @Override
    public List<T> getLeaderboard(String field, int limit, SortOrder order) {
        return findAllOrderedBy(field, order, limit);
    }

    @Override
    public List<T> getLeaderboardRange(String field, int startRank, int endRank, SortOrder order) {
        try {
            int size = endRank - startRank + 1;
            int page = startRank > 0 ? (startRank - 1) / size : 0;
            int offset = startRank > 0 ? (startRank - 1) % size : 0;
            
            List<T> pageResults = adapter.findAllPagedOrderedBy(entityClass, field, order, page, size + offset);
            
            if (pageResults.size() <= offset) {
                return new ArrayList<>();
            }
            
            int endIndex = Math.min(pageResults.size(), offset + size);
            return pageResults.subList(offset, endIndex);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("getLeaderboardRange", entityClass.getSimpleName(),
                    String.format("Failed to get leaderboard range from rank %d to %d for field '%s' with order %s", startRank, endRank, field, order), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public CompletableFuture<List<T>> getLeaderboardAsync(String field, int limit) {
        return getLeaderboardAsync(field, limit, SortOrder.DESC);
    }

    @Override
    public CompletableFuture<List<T>> getLeaderboardAsync(String field, int limit, SortOrder order) {
        return findAllOrderedByAsync(field, order, limit);
    }

    @Override
    public CompletableFuture<List<T>> getLeaderboardRangeAsync(String field, int startRank, int endRank, SortOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getLeaderboardRange(field, startRank, endRank, order);
            } catch (Exception e) {
                throw new RepositoryException("getLeaderboardRangeAsync", entityClass.getSimpleName(),
                        String.format("Failed in async getLeaderboardRange from rank %d to %d for field '%s' with order %s", startRank, endRank, field, order), e);
            }
        }, executor);
    }

    @Override
    public List<T> findByFieldOrderedBy(String filterField, Object filterValue, String orderField, SortOrder order, int limit) {
        try {
            return adapter.findByFieldOrderedBy(entityClass, filterField, filterValue, orderField, order, limit);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("findByFieldOrderedBy", entityClass.getSimpleName(),
                    String.format("Failed to find entities by field '%s' with value %s, ordered by '%s' %s with limit %d",
                            filterField, filterValue, orderField, order, limit), e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public long countByField(String field, Object value) {
        try {
            return adapter.countByField(entityClass, field, value);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("countByField", entityClass.getSimpleName(),
                    String.format("Failed to count entities by field '%s' with value %s", field, value), e);
            errorHandler.handleError(repoException);
            return 0;
        }
    }

    @Override
    public CompletableFuture<List<T>> findByFieldOrderedByAsync(String filterField, Object filterValue, String orderField, SortOrder order, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findByFieldOrderedBy(filterField, filterValue, orderField, order, limit);
            } catch (Exception e) {
                throw new RepositoryException("findByFieldOrderedByAsync", entityClass.getSimpleName(),
                        String.format("Failed in async findByFieldOrderedBy for field '%s' with value %s, ordered by '%s' %s with limit %d",
                                filterField, filterValue, orderField, order, limit), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Long> countByFieldAsync(String field, Object value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countByField(field, value);
            } catch (Exception e) {
                RepositoryException repoException = new RepositoryException("countByFieldAsync", entityClass.getSimpleName(),
                        String.format("Failed in async countByField for field '%s' with value %s", field, value), e);
                errorHandler.handleError(repoException);
                return 0L;
            }
        }, executor);
    }

    @Override
    public void drop() {
        try {
            adapter.dropTable(entityClass);
            adapter.createTable(entityClass);
        } catch (Exception e) {
            RepositoryException repoException = new RepositoryException("drop", entityClass.getSimpleName(),
                    "Failed to drop and recreate table/collection", e);
            errorHandler.handleError(repoException);
            throw repoException;
        }
    }

    @Override
    public CompletableFuture<Void> dropAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                drop();
            } catch (Exception e) {
                throw new RepositoryException("dropAsync", entityClass.getSimpleName(),
                        "Failed in async drop operation", e);
            }
        }, executor);
    }
}
