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

    <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findAllSortedPaged(String orderByField, boolean ascending, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    <T extends Entity> List<T> findByFieldSorted(String whereField, Object whereValue, String orderField, boolean ascending, int limit, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    String getAdapterName();

    boolean supportsBatchOperations();

    boolean supportsIndexes();

    boolean supportsTransactions();
}
