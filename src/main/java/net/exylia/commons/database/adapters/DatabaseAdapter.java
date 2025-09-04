package net.exylia.commons.database.adapters;

import net.exylia.commons.database.repository.Repository.SortOrder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DatabaseAdapter {

    void connect() throws Exception;
    void disconnect();
    boolean isConnected();

    // Operaciones CRUD básicas
    <T> void save(T entity) throws Exception;
    <T> void saveOrUpdateAll(List<T> entities) throws Exception;
    <T> void updateAll(List<T> entities) throws Exception;
    <T> void update(T entity) throws Exception;
    <T> void delete(T entity) throws Exception;
    <T> Optional<T> findById(Class<T> entityClass, Object id) throws Exception;
    <T> List<T> findAll(Class<T> entityClass) throws Exception;
    <T> List<T> findBy(Class<T> entityClass, String field, Object value) throws Exception;

    // Operaciones de consulta personalizada
    <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception;
    int executeUpdate(String query, Object... params) throws Exception;

    // Operaciones de esquema
    void createTable(Class<?> entityClass) throws Exception;
    void updateTable(Class<?> entityClass) throws Exception;
    boolean tableExists(Class<?> entityClass) throws Exception;
    List<String> getTableColumns(Class<?> entityClass) throws Exception;

    // Transacciones
    void beginTransaction() throws Exception;
    void commit() throws Exception;
    void rollback() throws Exception;

    // Métodos optimizados para grandes datasets
    <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order) throws Exception;
    <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order, int limit) throws Exception;
    <T> List<T> findAllPaged(Class<T> entityClass, int page, int size) throws Exception;
    <T> List<T> findAllPagedOrderedBy(Class<T> entityClass, String field, SortOrder order, int page, int size) throws Exception;
    <T> long getRankByField(Class<T> entityClass, String field, Object value, SortOrder order) throws Exception;
    <T> Optional<T> getByRank(Class<T> entityClass, String field, long rank, SortOrder order) throws Exception;

    // Utilidades
    String getTableName(Class<?> entityClass);
    Map<String, Object> entityToMap(Object entity) throws Exception;
    <T> T mapToEntity(Map<String, Object> map, Class<T> entityClass) throws Exception;
}