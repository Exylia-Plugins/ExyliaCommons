package net.exylia.commons.database.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<T> {

    void saveOrUpdate(T entity);
    void saveOrUpdateAll(List<T> entities);
    void delete(T entity);
    void deleteAll(List<T> entities);

    Optional<T> findById(Object id);
    List<T> findAll();
    List<T> findBy(String field, Object value);
    boolean exists(Object id);
    long count();

    CompletableFuture<Void> saveOrUpdateAsync(T entity);
    CompletableFuture<Void> saveOrUpdateAllAsync(List<T> entities);
    CompletableFuture<Void> deleteAsync(T entity);
    CompletableFuture<Void> deleteAllAsync(List<T> entities);

    CompletableFuture<Optional<T>> findByIdAsync(Object id);
    CompletableFuture<List<T>> findAllAsync();
    CompletableFuture<List<T>> findByAsync(String field, Object value);
    CompletableFuture<Boolean> existsAsync(Object id);
    CompletableFuture<Long> countAsync();

    List<T> query(String query, Object... params);
    CompletableFuture<List<T>> queryAsync(String query, Object... params);

    List<T> findAllOrderedBy(String field, SortOrder order);
    List<T> findAllOrderedBy(String field, SortOrder order, int limit);
    List<T> findTopN(String field, int n);
    List<T> findBottomN(String field, int n);
    CompletableFuture<List<T>> findAllOrderedByAsync(String field, SortOrder order);
    CompletableFuture<List<T>> findAllOrderedByAsync(String field, SortOrder order, int limit);
    CompletableFuture<List<T>> findTopNAsync(String field, int n);
    CompletableFuture<List<T>> findBottomNAsync(String field, int n);

    List<T> findAllPaged(int page, int size);
    List<T> findAllPagedOrderedBy(String field, SortOrder order, int page, int size);
    CompletableFuture<List<T>> findAllPagedAsync(int page, int size);
    CompletableFuture<List<T>> findAllPagedOrderedByAsync(String field, SortOrder order, int page, int size);

    long getRankByField(String field, Object value, SortOrder order);
    Optional<T> getByRank(String field, long rank, SortOrder order);
    CompletableFuture<Long> getRankByFieldAsync(String field, Object value, SortOrder order);
    CompletableFuture<Optional<T>> getByRankAsync(String field, long rank, SortOrder order);

    List<T> getLeaderboard(String field, int limit);
    List<T> getLeaderboard(String field, int limit, SortOrder order);
    List<T> getLeaderboardRange(String field, int startRank, int endRank, SortOrder order);
    CompletableFuture<List<T>> getLeaderboardAsync(String field, int limit);
    CompletableFuture<List<T>> getLeaderboardAsync(String field, int limit, SortOrder order);
    CompletableFuture<List<T>> getLeaderboardRangeAsync(String field, int startRank, int endRank, SortOrder order);

    List<T> findByFieldOrderedBy(String filterField, Object filterValue, String orderField, SortOrder order, int limit);
    long countByField(String field, Object value);
    CompletableFuture<List<T>> findByFieldOrderedByAsync(String filterField, Object filterValue, String orderField, SortOrder order, int limit);
    CompletableFuture<Long> countByFieldAsync(String field, Object value);

    void drop();
    CompletableFuture<Void> dropAsync();

    enum SortOrder {
        ASC, DESC
    }
}
