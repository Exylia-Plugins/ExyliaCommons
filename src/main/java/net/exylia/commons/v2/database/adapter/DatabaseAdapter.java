package net.exylia.commons.v2.database.adapter;

import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;

import java.util.List;
import java.util.Optional;

public interface DatabaseAdapter {

    void connect() throws Exception;

    void disconnect() throws Exception;

    default void reconnect() throws Exception {
        disconnect();
        connect();
    }

    boolean isConnected();

    void createTable(EntityMetadata metadata) throws Exception;

    void updateTable(EntityMetadata metadata) throws Exception;

    boolean tableExists(EntityMetadata metadata) throws Exception;

    <T extends Entity> void insert(T entity, EntityMetadata metadata) throws Exception;

    <T extends Entity> void update(T entity, EntityMetadata metadata) throws Exception;

    <T extends Entity> void delete(T entity, EntityMetadata metadata) throws Exception;

    <T extends Entity> Optional<T> findById(Object id, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findAll(Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> long count(Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> long countByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> executeQuery(String query, List<Object> params, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findByFieldPaged(String fieldName, Object value, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> void insertBatch(List<T> entities, EntityMetadata metadata) throws Exception;

    <T extends Entity> void updateBatch(List<T> entities, EntityMetadata metadata) throws Exception;

    <T extends Entity> void upsertBatch(List<T> entities, EntityMetadata metadata) throws Exception;

    <T extends Entity> void deleteBatch(List<T> entities, EntityMetadata metadata) throws Exception;

    /**
     * Wipes every row/document of this entity's table in a single operation
     * (e.g. {@code DELETE FROM table} with no WHERE clause, or an equivalent
     * bulk primitive), instead of loading and deleting entities one by one.
     * Use this for "clear everything" operations — it is orders of magnitude
     * faster than paginating + deleting per-row once the table has a
     * non-trivial number of entries. Returns the number of rows removed.
     */
    int truncate(EntityMetadata metadata) throws Exception;

    /**
     * Deletes at most {@code limit} rows whose {@code field} compares strictly
     * less than {@code value}, oldest first, in a single server-side statement.
     * <p>
     * This exists because the alternative — SELECT a page, map it to entities,
     * then delete them one by one — has to materialise every column of every
     * matched row just to throw it away. On a table carrying a large TEXT/BLOB
     * column (serialized inventories, snapshots, payloads) that is megabytes of
     * garbage per batch and is a reliable way to stall a server. Nothing is
     * loaded here; the rows never leave the database.
     * <p>
     * The {@code limit} bounds the transaction so a purge of a huge table stays
     * a sequence of small commits rather than one lock-holding monster. Call it
     * in a loop until it returns less than {@code limit}.
     *
     * @param field entity field name (not the column name) to compare on
     * @param value exclusive upper bound; rows strictly below it are deleted
     * @param limit maximum rows to delete in this call
     * @return number of rows actually deleted
     */
    int deleteWhereLessThan(String field, Object value, int limit, EntityMetadata metadata) throws Exception;

    /**
     * Deletes at most {@code limit} rows, unconditionally. Used to drain a table
     * in bounded chunks when a plain {@code truncate()} would build an undo log
     * large enough to be its own outage. Call in a loop until it returns 0.
     *
     * @return number of rows actually deleted
     */
    int deleteBounded(int limit, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findAllSortedPaged(String orderByField, boolean ascending, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findByFieldSorted(String whereField, Object whereValue, String orderField, boolean ascending, int limit, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    String getAdapterName();

    boolean supportsBatchOperations();

    boolean supportsIndexes();

    boolean supportsTransactions();
}
