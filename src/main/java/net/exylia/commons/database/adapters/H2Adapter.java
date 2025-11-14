package net.exylia.commons.database.adapters;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.exceptions.ConnectionException;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;
import net.exylia.commons.database.exceptions.DatabaseException;
import net.exylia.commons.database.exceptions.SerializationException;
import net.exylia.commons.database.serialization.CollectionUtils;
import net.exylia.commons.database.serialization.EnumSafetyHandler;
import net.exylia.commons.database.serialization.SerializationHelper;
import org.bukkit.configuration.file.FileConfiguration;
import net.exylia.commons.database.repository.Repository.SortOrder;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

public class H2Adapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private final DatabaseErrorHandler errorHandler;
    private HikariDataSource dataSource;

    public H2Adapter(FileConfiguration config, ExyliaPlugin plugin, DatabaseErrorHandler errorHandler) {
        this.config = config;
        this.plugin = plugin;
        this.errorHandler = errorHandler;
    }

    @Override
    public void connect() throws Exception {
        String fileName = config.getString("database.h2.file", "database/h2");
        String username = config.getString("database.h2.username", "sa");
        String password = config.getString("database.h2.password", "");
        int poolSize = config.getInt("database.h2.pool-size", 5);

        String url = "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/" + fileName + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

        try {
             
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ConnectionException("H2", url, "H2 driver not found in classpath", e);
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("org.h2.Driver");
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(900000);
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "100");
            hikariConfig.setPoolName("ExyliaDB-H2-Pool");

            dataSource = new HikariDataSource(hikariConfig);

            try (Connection testConnection = dataSource.getConnection()) {
                testConnection.setAutoCommit(true);
                logInternalInfo("H2 connection pool established with " + poolSize + " connections.");
            }

        } catch (Exception e) {
            throw new ConnectionException("H2", url, "Failed to establish H2 connection pool", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                 
                dataSource.getHikariPoolMXBean().softEvictConnections();

                Thread.sleep(1000);

                dataSource.close();
                logInternalInfo("H2 connection pool closed successfully");
            }
        } catch (Exception e) {
            errorHandler.logWarning("Disconnect", "H2Adapter", "Error closing H2 connection pool: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return dataSource != null && !dataSource.isClosed() && dataSource.getConnection().isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            if (dataSource == null || dataSource.isClosed()) {
                throw new ConnectionException("H2", "pool", "DataSource is null or closed", null);
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            if (e.getMessage().contains("has been closed")) {
                throw new ConnectionException("H2", "pool", "Connection pool is closed - reload in progress", e);
            }
            throw new ConnectionException("H2", "pool", "Failed to get connection from pool", e);
        }
    }

    @Override
    public <T> T save(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            String tableName = getTableName(entity.getClass());
            Map<String, Object> values = entityToMap(entity);

            StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
            StringBuilder valuePlaceholders = new StringBuilder("VALUES (");

            List<Object> parameters = new ArrayList<>();
            boolean first = true;

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!first) {
                    sql.append(", ");
                    valuePlaceholders.append(", ");
                }
                sql.append(entry.getKey());
                valuePlaceholders.append("?");
                parameters.add(entry.getValue());
                first = false;
            }

            sql.append(") ").append(valuePlaceholders).append(")");

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {

                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }

                int result = stmt.executeUpdate();
                if (result == 0) {
                    throw new DatabaseException("Save", entityClassName, "H2",
                            "No rows were inserted, save operation failed");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        setPrimaryKeyValue(entity, generatedId);
                    }
                }

                return entity;

            } catch (SQLException e) {
                throw new DatabaseException("Save", entityClassName, "H2",
                        "SQL error during save operation: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Save", entityClassName, "H2",
                        "Unexpected error during save operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> saveOrUpdateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        String entityClassName = entities.get(0).getClass().getSimpleName();

        try {
            String tableName = getTableName(entities.get(0).getClass());
            String primaryKey = getPrimaryKeyField(entities.get(0).getClass());

            List<T> newEntities = new ArrayList<>();
            List<T> existingEntities = new ArrayList<>();

            for (T entity : entities) {
                Map<String, Object> entityMap = entityToMap(entity);
                Object pkValue = getPrimaryKeyValue(entity);

                boolean isNew = pkValue == null ||
                               (pkValue instanceof Integer && (Integer)pkValue == 0) ||
                               (pkValue instanceof Long && (Long)pkValue == 0L);

                if (isNew) {
                    newEntities.add(entity);
                } else {
                    existingEntities.add(entity);
                }
            }

            int successCount = 0;
            int failureCount = 0;

            try (Connection conn = getConnection()) {
                 
                if (!newEntities.isEmpty()) {
                    Map<String, Object> firstEntityMap = entityToMap(newEntities.get(0));

                    StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
                    StringBuilder valuePlaceholders = new StringBuilder("VALUES (");
                    List<String> columns = new ArrayList<>();

                    boolean first = true;
                    for (String columnName : firstEntityMap.keySet()) {
                        if (!first) {
                            insertSql.append(", ");
                            valuePlaceholders.append(", ");
                        }
                        insertSql.append(columnName);
                        valuePlaceholders.append("?");
                        columns.add(columnName);
                        first = false;
                    }

                    insertSql.append(") ").append(valuePlaceholders).append(")");

                    try (PreparedStatement stmt = conn.prepareStatement(insertSql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                        for (T entity : newEntities) {
                            try {
                                Map<String, Object> entityMap = entityToMap(entity);
                                for (int i = 0; i < columns.size(); i++) {
                                    stmt.setObject(i + 1, entityMap.get(columns.get(i)));
                                }
                                stmt.addBatch();
                                successCount++;
                            } catch (Exception e) {
                                failureCount++;
                                errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                                        "Failed to prepare new entity for batch: " + e.getMessage());
                            }
                        }
                        stmt.executeBatch();

                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            int index = 0;
                            while (generatedKeys.next() && index < newEntities.size()) {
                                T entity = newEntities.get(index);
                                int generatedId = generatedKeys.getInt(1);
                                setPrimaryKeyValue(entity, generatedId);
                                index++;
                            }
                        }
                    }
                }

                if (!existingEntities.isEmpty()) {
                    Map<String, Object> firstEntityMap = entityToMapWithId(existingEntities.get(0));

                    StringBuilder mergeSql = new StringBuilder("MERGE INTO " + tableName + " (");
                    StringBuilder valuePlaceholders = new StringBuilder("VALUES (");
                    List<String> columns = new ArrayList<>();

                    boolean first = true;
                    for (String columnName : firstEntityMap.keySet()) {
                        if (!first) {
                            mergeSql.append(", ");
                            valuePlaceholders.append(", ");
                        }
                        mergeSql.append(columnName);
                        valuePlaceholders.append("?");
                        columns.add(columnName);
                        first = false;
                    }

                    mergeSql.append(") ").append(valuePlaceholders).append(")");

                    try (PreparedStatement stmt = conn.prepareStatement(mergeSql.toString())) {
                        for (T entity : existingEntities) {
                            try {
                                Map<String, Object> entityMap = entityToMapWithId(entity);
                                for (int i = 0; i < columns.size(); i++) {
                                    stmt.setObject(i + 1, entityMap.get(columns.get(i)));
                                }
                                stmt.addBatch();
                                successCount++;
                            } catch (Exception e) {
                                failureCount++;
                                errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                                        "Failed to prepare existing entity for batch: " + e.getMessage());
                            }
                        }
                        stmt.executeBatch();
                    }
                }

                int[] results = new int[successCount];
                int processed = 0;
                for (int result : results) {
                    if (result >= 0) processed++;  
                }

                if (failureCount > 0) {
                    errorHandler.logWarning("SaveOrUpdateAll", entityClassName,
                            String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
                }

                return entities;

            } catch (SQLException e) {
                throw new DatabaseException("SaveOrUpdateAll", entityClassName, "H2",
                        "SQL error during batch saveOrUpdate operation", e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("SaveOrUpdateAll", entityClassName, "H2",
                        "Unexpected error during batch saveOrUpdate operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> updateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        String entityClassName = entities.get(0).getClass().getSimpleName();

        try {
            String tableName = getTableName(entities.get(0).getClass());
            String primaryKey = getPrimaryKeyField(entities.get(0).getClass());

            Map<String, Object> firstEntityMap = entityToMap(entities.get(0));

            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
            List<String> updateColumns = new ArrayList<>();

            boolean first = true;
            for (String column : firstEntityMap.keySet()) {
                if (!column.equals(primaryKey)) {
                    if (!first) {
                        sql.append(", ");
                    }
                    sql.append(column).append(" = ?");
                    updateColumns.add(column);
                    first = false;
                }
            }

            sql.append(" WHERE ").append(primaryKey).append(" = ?");

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                int successCount = 0;
                int failureCount = 0;

                for (T entity : entities) {
                    try {
                        Map<String, Object> entityMap = entityToMap(entity);

                        int paramIndex = 1;
                         
                        for (String column : updateColumns) {
                            stmt.setObject(paramIndex++, entityMap.get(column));
                        }
                         
                        stmt.setObject(paramIndex, entityMap.get(primaryKey));

                        stmt.addBatch();
                        successCount++;
                    } catch (Exception e) {
                        failureCount++;
                        errorHandler.logWarning("UpdateAll", entityClassName,
                                "Failed to prepare entity for batch update: " + e.getMessage());
                    }
                }

                int[] results = stmt.executeBatch();
                int updated = 0;
                for (int result : results) {
                    if (result > 0) updated++;
                }

                logInternalInfo("updateAll completed: " + updated + " of " + entities.size() + " entities updated");

                if (failureCount > 0) {
                    errorHandler.logWarning("UpdateAll", entityClassName,
                            String.format("Completed with %d failures out of %d entities", failureCount, entities.size()));
                }

                return entities;

            } catch (SQLException e) {
                throw new DatabaseException("UpdateAll", entityClassName, "H2",
                        "SQL error during batch update operation", e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateAll", entityClassName, "H2",
                        "Unexpected error during batch update operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> T update(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            String tableName = getTableName(entity.getClass());
            Map<String, Object> values = entityToMap(entity);

            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = values.get(primaryKey);

            if (primaryKeyValue == null) {
                throw new DatabaseException("Update", entityClassName, "H2",
                        "Cannot update entity without primary key value");
            }

            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
            List<Object> parameters = new ArrayList<>();
            boolean first = true;

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!entry.getKey().equals(primaryKey)) {
                    if (!first) {
                        sql.append(", ");
                    }
                    sql.append(entry.getKey()).append(" = ?");
                    parameters.add(entry.getValue());
                    first = false;
                }
            }

            sql.append(" WHERE ").append(primaryKey).append(" = ?");
            parameters.add(primaryKeyValue);

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }

                int result = stmt.executeUpdate();
                if (result == 0) {
                    errorHandler.logWarning("Update", entityClassName,
                            "No rows were updated for primary key: " + primaryKeyValue);
                }

                return entity;

            } catch (SQLException e) {
                throw new DatabaseException("Update", entityClassName, "H2",
                        "SQL error during update operation for primary key: " + primaryKeyValue, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Update", entityClassName, "H2",
                        "Unexpected error during update operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> boolean delete(T entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();

        try {
            String tableName = getTableName(entity.getClass());
            String primaryKey = getPrimaryKeyField(entity.getClass());
            Object primaryKeyValue = entityToMap(entity).get(primaryKey);

            if (primaryKeyValue == null) {
                throw new DatabaseException("Delete", entityClassName, "H2",
                        "Cannot delete entity without primary key value");
            }

            String sql = "DELETE FROM " + tableName + " WHERE " + primaryKey + " = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, primaryKeyValue);
                int result = stmt.executeUpdate();

                if (result == 0) {
                    errorHandler.logWarning("Delete", entityClassName,
                            "No rows were deleted for primary key: " + primaryKeyValue);
                    return false;
                }

                return true;

            } catch (SQLException e) {
                throw new DatabaseException("Delete", entityClassName, "H2",
                        "SQL error during delete operation for primary key: " + primaryKeyValue, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("Delete", entityClassName, "H2",
                        "Unexpected error during delete operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityClass, Object id) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String primaryKey = getPrimaryKeyField(entityClass);

            String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKey + " = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, id);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    try {
                        T entity = mapToEntity(resultSetToMap(rs), entityClass);
                        return Optional.of(entity);
                    } catch (Exception e) {
                        throw new DatabaseException("FindById", entityClassName, "H2",
                                "Failed to map result set to entity for ID: " + id, e);
                    }
                }

                return Optional.empty();

            } catch (SQLException e) {
                throw new DatabaseException("FindById", entityClassName, "H2",
                        "SQL error during findById operation for ID: " + id, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindById", entityClassName, "H2",
                        "Unexpected error during findById operation for ID: " + id, e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "SELECT * FROM " + tableName;

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAll", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindAll", entityClassName, "H2",
                        "SQL error during findAll operation", e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAll", entityClassName, "H2",
                        "Unexpected error during findAll operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findBy(Class<T> entityClass, String field, Object value) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "SELECT * FROM " + tableName + " WHERE " + field + " = ?";

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, value);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindBy", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindBy", entityClassName, "H2",
                        String.format("SQL error during findBy operation for field '%s' with value: %s", field, value), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindBy", entityClassName, "H2",
                        String.format("Unexpected error during findBy operation for field '%s'", field), e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("ExecuteQuery", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("ExecuteQuery", entityClassName, "H2",
                        "SQL error during custom query execution: " + query, e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("ExecuteQuery", entityClassName, "H2",
                        "Unexpected error during custom query execution", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public int executeUpdate(String query, Object... params) throws Exception {
        try {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return stmt.executeUpdate();

            } catch (SQLException e) {
                throw new DatabaseException("ExecuteUpdate", "Unknown", "H2",
                        "SQL error during update query execution: " + query, e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("ExecuteUpdate", "Unknown", "H2",
                        "Unexpected error during update query execution", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

            Field[] fields = entityClass.getDeclaredFields();
            List<String> primaryKeyColumns = new ArrayList<>();
            boolean first = true;

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    if (column.primaryKey()) {
                        String columnName = column.name().isEmpty() ? field.getName() : column.name();
                        primaryKeyColumns.add(columnName);
                    }
                }
            }

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    if (!first) sql.append(", ");

                    Column column = field.getAnnotation(Column.class);
                    String columnName = column.name().isEmpty() ? field.getName() : column.name();
                    String sqlType = getSQLType(field.getType(), column);

                    sql.append(columnName).append(" ").append(sqlType);

                    if (column.primaryKey() && primaryKeyColumns.size() == 1) {
                        sql.append(" PRIMARY KEY");
                        if (column.autoIncrement()) {
                            sql.append(" AUTO_INCREMENT");
                        }
                    }

                    if (!column.nullable() && !column.primaryKey()) {
                        sql.append(" NOT NULL");
                    }

                    if (column.unique() && !column.primaryKey()) {
                        sql.append(" UNIQUE");
                    }

                    if (!column.defaultValue().isEmpty()) {
                        sql.append(" DEFAULT '").append(column.defaultValue()).append("'");
                    }

                    first = false;
                }
            }

            if (primaryKeyColumns.size() > 1) {
                sql.append(", PRIMARY KEY (");
                for (int i = 0; i < primaryKeyColumns.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append(primaryKeyColumns.get(i));
                }
                sql.append(")");
            }

            sql.append(")");

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql.toString());
                logInternalInfo("Table created successfully: " + tableName);
            } catch (SQLException e) {
                throw new DatabaseException("CreateTable", entityClassName, "H2",
                        "SQL error during table creation: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("CreateTable", entityClassName, "H2",
                        "Unexpected error during table creation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void updateTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            if (!tableExists(entityClass)) {
                createTable(entityClass);
                return;
            }

            List<String> existingColumns = getTableColumns(entityClass);
            Field[] fields = entityClass.getDeclaredFields();
            boolean hasUpdates = false;

            try (Connection conn = getConnection()) {
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Column.class)) {
                        Column column = field.getAnnotation(Column.class);
                        String columnName = column.name().isEmpty() ? field.getName() : column.name();

                        if (!existingColumns.contains(columnName.toLowerCase())) {
                            String sqlType = getSQLType(field.getType(), column);
                            String alterSql = "ALTER TABLE " + getTableName(entityClass) +
                                    " ADD COLUMN " + columnName + " " + sqlType;

                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(alterSql);
                                logInternalInfo("Column added (nullable): " + columnName);
                                hasUpdates = true;
                            }

                            if (!column.defaultValue().isEmpty()) {
                                String updateSql = "UPDATE " + getTableName(entityClass) +
                                        " SET " + columnName + " = ? WHERE " + columnName + " IS NULL";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, column.defaultValue());
                                    int updated = updateStmt.executeUpdate();
                                    logInternalInfo("Updated " + updated + " records with default value for " + columnName);
                                }
                            }

                            if (!column.nullable() && !column.primaryKey()) {
                                String alterNotNullSql = "ALTER TABLE " + getTableName(entityClass) +
                                        " ALTER COLUMN " + columnName + " SET NOT NULL";
                                try (Statement stmt = conn.createStatement()) {
                                    stmt.execute(alterNotNullSql);
                                    logInternalInfo("Column configured as NOT NULL: " + columnName);
                                }
                            }
                        }
                    }
                }

                if (hasUpdates) {
                    logInternalInfo("Table update completed: " + getTableName(entityClass));
                } else {
                    logInternalInfo("No updates required for table: " + getTableName(entityClass));
                }

            } catch (SQLException e) {
                throw new DatabaseException("UpdateTable", entityClassName, "H2",
                        "SQL error during table update: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("UpdateTable", entityClassName, "H2",
                        "Unexpected error during table update", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                ResultSet rs = stmt.executeQuery();
                rs.next();
                return rs.getInt(1) > 0;
            } catch (SQLException e) {
                throw new DatabaseException("TableExists", entityClassName, "H2",
                        "SQL error while checking table existence: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("TableExists", entityClassName, "H2",
                        "Unexpected error while checking table existence", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?";

            List<String> columns = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName.toUpperCase());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            } catch (SQLException e) {
                throw new DatabaseException("GetTableColumns", entityClassName, "H2",
                        "SQL error while getting table columns: " + e.getMessage(), e);
            }

            return columns;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("GetTableColumns", entityClassName, "H2",
                        "Unexpected error while getting table columns", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public void beginTransaction() throws Exception {
        throw new UnsupportedOperationException("Transactions per pool require specific implementation");
    }

    @Override
    public void commit() throws Exception {
        throw new UnsupportedOperationException("Transactions per pool require specific implementation");
    }

    @Override
    public void rollback() throws Exception {
        throw new UnsupportedOperationException("Transactions per pool require specific implementation");
    }

    @Override
    public String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    @Override
    public Map<String, Object> entityToMap(Object entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();
        Map<String, Object> map = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();

                try {
                    Object value = field.get(entity);

                    if (column.autoIncrement() && value != null) {
                        if ((value instanceof Integer && (Integer) value == 0) ||
                            (value instanceof Long && (Long) value == 0L)) {
                            continue;
                        }
                    }

                    if (value != null && value.getClass().isEnum()) {
                        value = ((Enum<?>) value).name();
                    } else if (value instanceof java.util.UUID) {
                        value = value.toString();
                    } else if (value != null && column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                        } catch (SerializationException e) {
                            errorHandler.handleError(e);
                            throw e;
                        } catch (Exception e) {
                            SerializationException serException = new SerializationException("Serialize",
                                    entityClassName, columnName, column.serializationType().toString(), value,
                                    "Failed to auto-serialize field during entityToMap", e);
                            errorHandler.handleError(serException);
                            throw serException;
                        }
                    }

                    map.put(columnName, value);

                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToMap", entityClassName, "H2",
                            "Failed to access field: " + columnName, e);
                }
            }
        }

        return map;
    }

    private Map<String, Object> entityToMapWithId(Object entity) throws Exception {
        String entityClassName = entity.getClass().getSimpleName();
        Map<String, Object> map = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();

                try {
                    Object value = field.get(entity);

                    if (value != null && value.getClass().isEnum()) {
                        value = ((Enum<?>) value).name();
                    } else if (value instanceof java.util.UUID) {
                        value = value.toString();
                    } else if (value != null && column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                        } catch (SerializationException e) {
                            errorHandler.handleError(e);
                            throw e;
                        } catch (Exception e) {
                            SerializationException serException = new SerializationException("Serialize",
                                    entityClassName, columnName, column.serializationType().toString(), value,
                                    "Failed to auto-serialize field during entityToMapWithId", e);
                            errorHandler.handleError(serException);
                            throw serException;
                        }
                    }

                    map.put(columnName, value);

                } catch (IllegalAccessException e) {
                    throw new DatabaseException("EntityToMapWithId", entityClassName, "H2",
                            "Failed to access field: " + columnName, e);
                }
            }
        }

        return map;
    }

    private Object getPrimaryKeyValue(Object entity) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
        }
        return null;
    }

    private void setPrimaryKeyValue(Object entity, Object value) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                     
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(entity, ((Number) value).intValue());
                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                        field.set(entity, ((Number) value).longValue());
                    } else {
                        field.set(entity, value);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public <T> T mapToEntity(Map<String, Object> map, Class<T> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            Field[] fields = entityClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    String columnName = column.name().isEmpty() ? field.getName() : column.name();

                    Object value = map.get(columnName);

                    if (value == null && CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                        try {
                            Object emptyCollection = CollectionUtils.createEmptyCollection(field);
                            if (emptyCollection != null) {
                                field.set(entity, emptyCollection);
                                continue;  
                            }
                        } catch (Exception e) {
                            errorHandler.logWarning("MapToEntity", entityClassName,
                                    "Failed to initialize empty collection for field " + columnName + ": " + e.getMessage());
                        }
                    }

                    if (value != null) {
                        try {

                            if (field.getType().isEnum() && value instanceof String) {
                                value = EnumSafetyHandler.handleEnumDeserialization(value, field,
                                        entityClassName, columnName, errorHandler);
                            }
                            else if (column.autoSerialize()) {
                                try {
                                    value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                                } catch (SerializationException e) {
                                    errorHandler.handleError(e);

                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logWarning("MapToEntity", entityClassName,
                                                "Deserialization failed for collection " + columnName + ", initializing empty collection");
                                        value = CollectionUtils.createEmptyCollection(field);
                                    } else {
                                        throw e;
                                    }
                                } catch (Exception e) {
                                    SerializationException serException = new SerializationException("Deserialize",
                                            entityClassName, columnName, column.serializationType().toString(), value,
                                            "Failed to auto-deserialize field during mapToEntity", e);
                                    errorHandler.handleError(serException);

                                    if (CollectionUtils.isCollectionType(field.getType()) && column.initializeEmpty()) {
                                        errorHandler.logWarning("MapToEntity", entityClassName,
                                                "Deserialization failed for collection " + columnName + ", initializing empty collection");
                                        value = CollectionUtils.createEmptyCollection(field);
                                    } else {
                                        throw serException;
                                    }
                                }
                            } else {
                                value = convertValue(value, field.getType());
                            }

                            field.set(entity, value);

                        } catch (IllegalAccessException e) {
                            throw new DatabaseException("MapToEntity", entityClassName, getAdapterType(),
                                    "Failed to set field value: " + columnName, e);
                        }
                    }
                }
            }

            return entity;

        } catch (Exception e) {
            if (e instanceof DatabaseException || e instanceof SerializationException) {
                throw e;
            } else {
                throw new DatabaseException("MapToEntity", entityClassName, getAdapterType(),
                        "Failed to create entity instance or map fields", e);
            }
        }
    }

    private String getAdapterType() {
        return this.getClass().getSimpleName().replace("Adapter", "");
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

    private String getSQLType(Class<?> javaType, Column column) {
         
        if (column.autoSerialize()) {
            if (column.length() == -1 || column.length() > 8000) {
                return "TEXT";
            } else if (column.length() > 255) {
                return "TEXT";
            } else {
                return "VARCHAR(" + Math.max(column.length(), 500) + ")";  
            }
        }

        if (javaType == String.class) {
            if (column.length() == -1 || column.length() > 8000) {
                return "TEXT";
            }
            if (column.length() <= 0) {
                return "VARCHAR(255)";
            }
            return "VARCHAR(" + column.length() + ")";
        } else if (javaType == int.class || javaType == Integer.class) {
            return "INT";
        } else if (javaType == long.class || javaType == Long.class) {
            return "BIGINT";
        } else if (javaType == double.class || javaType == Double.class) {
            return "DOUBLE";
        } else if (javaType == float.class || javaType == Float.class) {
            return "FLOAT";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "BOOLEAN";
        } else if (javaType == Date.class || javaType == java.sql.Date.class) {
            return "TIMESTAMP";
        } else if (javaType == java.util.UUID.class) {
            return "CHAR(36)";
        } else {
            return "TEXT";
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        try {
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == int.class || targetType == Integer.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                String strValue = value.toString();
                if (strValue.contains(".")) {
                    return Double.valueOf(strValue).intValue();
                }
                return Integer.valueOf(strValue);
            } else if (targetType == long.class || targetType == Long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                String strValue = value.toString();
                if (strValue.contains(".")) {
                    return Double.valueOf(strValue).longValue();
                }
                return Long.valueOf(strValue);
            } else if (targetType == double.class || targetType == Double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.valueOf(value.toString());
            } else if (targetType == float.class || targetType == Float.class) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                return Float.valueOf(value.toString());
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                return Boolean.valueOf(value.toString());
            } else if (targetType == java.util.UUID.class) {
                if (value instanceof String) {
                    return java.util.UUID.fromString((String) value);
                }
                return java.util.UUID.fromString(value.toString());
            } else if (targetType.isEnum() && value instanceof String) {
                @SuppressWarnings("unchecked")
                Class<Enum> enumClass = (Class<Enum>) targetType;
                return Enum.valueOf(enumClass, (String) value);
            }

            return value;
        } catch (Exception e) {
            throw new DatabaseException("Value Conversion", "Unknown", "H2",
                    String.format("Failed to convert value '%s' to type %s", value, targetType.getSimpleName()), e);
        }
    }

    @Override
    public <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String orderDirection = (order == SortOrder.DESC) ? "DESC" : "ASC";
            String sql = "SELECT * FROM " + tableName + " ORDER BY " + field + " " + orderDirection;

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllOrderedBy", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindAllOrderedBy", entityClassName, "H2",
                        String.format("SQL error during ordered query for field '%s'", field), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllOrderedBy", entityClassName, "H2",
                        "Unexpected error during ordered query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllOrderedBy(Class<T> entityClass, String field, SortOrder order, int limit) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String orderDirection = (order == SortOrder.DESC) ? "DESC" : "ASC";
            String sql = "SELECT * FROM " + tableName + " ORDER BY " + field + " " + orderDirection + " LIMIT ?";

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllOrderedBy", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindAllOrderedBy", entityClassName, "H2",
                        String.format("SQL error during limited ordered query for field '%s' with limit %d", field, limit), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllOrderedBy", entityClassName, "H2",
                        "Unexpected error during limited ordered query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllPaged(Class<T> entityClass, int page, int size) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            int offset = page * size;
            String sql = "SELECT * FROM " + tableName + " LIMIT ? OFFSET ?";

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, size);
                stmt.setInt(2, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllPaged", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindAllPaged", entityClassName, "H2",
                        String.format("SQL error during paged query: page %d, size %d", page, size), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllPaged", entityClassName, "H2",
                        "Unexpected error during paged query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findAllPagedOrderedBy(Class<T> entityClass, String field, SortOrder order, int page, int size) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String orderDirection = (order == SortOrder.DESC) ? "DESC" : "ASC";
            int offset = page * size;
            String sql = "SELECT * FROM " + tableName + " ORDER BY " + field + " " + orderDirection + " LIMIT ? OFFSET ?";

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, size);
                stmt.setInt(2, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindAllPagedOrderedBy", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindAllPagedOrderedBy", entityClassName, "H2",
                        String.format("SQL error during paged ordered query: field '%s', page %d, size %d", field, page, size), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindAllPagedOrderedBy", entityClassName, "H2",
                        "Unexpected error during paged ordered query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> long getRankByField(Class<T> entityClass, String field, Object value, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String operator = (order == SortOrder.DESC) ? ">" : "<";
            String sql = "SELECT COUNT(*) + 1 FROM " + tableName + " WHERE " + field + " " + operator + " ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, value);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getLong(1);
                }

                return -1;

            } catch (SQLException e) {
                throw new DatabaseException("GetRankByField", entityClassName, "H2",
                        String.format("SQL error during rank calculation for field '%s' with value %s", field, value), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("GetRankByField", entityClassName, "H2",
                        "Unexpected error during rank calculation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> Optional<T> getByRank(Class<T> entityClass, String field, long rank, SortOrder order) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String orderDirection = (order == SortOrder.DESC) ? "DESC" : "ASC";
            long offset = Math.max(0, rank - 1);
            String sql = "SELECT * FROM " + tableName + " ORDER BY " + field + " " + orderDirection + " LIMIT 1 OFFSET ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, offset);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    try {
                        T entity = mapToEntity(resultSetToMap(rs), entityClass);
                        return Optional.of(entity);
                    } catch (Exception e) {
                        throw new DatabaseException("GetByRank", entityClassName, "H2",
                                "Failed to map result set to entity for rank: " + rank, e);
                    }
                }

                return Optional.empty();

            } catch (SQLException e) {
                throw new DatabaseException("GetByRank", entityClassName, "H2",
                        String.format("SQL error during rank query for rank %d", rank), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("GetByRank", entityClassName, "H2",
                        "Unexpected error during rank query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> List<T> findByFieldOrderedBy(Class<T> entityClass, String filterField, Object filterValue,
                                            String orderField, SortOrder order, int limit) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String orderDirection = (order == SortOrder.DESC) ? "DESC" : "ASC";
            String sql = "SELECT * FROM " + tableName + " WHERE " + filterField + " = ? ORDER BY " + orderField + " " + orderDirection + " LIMIT ?";

            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, filterValue);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    try {
                        results.add(mapToEntity(resultSetToMap(rs), entityClass));
                    } catch (Exception e) {
                        errorHandler.logWarning("FindByFieldOrderedBy", entityClassName,
                                "Failed to map one result to entity: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                throw new DatabaseException("FindByFieldOrderedBy", entityClassName, "H2",
                        String.format("SQL error during filtered ordered query: filter '%s'='%s', order '%s', limit %d",
                                filterField, filterValue, orderField, limit), e);
            }

            return results;

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("FindByFieldOrderedBy", entityClassName, "H2",
                        "Unexpected error during filtered ordered query operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    @Override
    public <T> long countByField(Class<T> entityClass, String field, Object value) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + field + " = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, value);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getLong(1);
                }

                return 0;

            } catch (SQLException e) {
                throw new DatabaseException("CountByField", entityClassName, "H2",
                        String.format("SQL error during count for field '%s' with value %s", field, value), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("CountByField", entityClassName, "H2",
                        "Unexpected error during count operation", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i).toLowerCase();
            Object value = rs.getObject(i);
            map.put(columnName, value);
        }

        return map;
    }

    @Override
    public void dropTable(Class<?> entityClass) throws Exception {
        String entityClassName = entityClass.getSimpleName();

        try {
            String tableName = getTableName(entityClass);
            String sql = "DROP TABLE IF EXISTS " + tableName;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                logInternalInfo("Table dropped successfully: " + tableName);
            } catch (SQLException e) {
                throw new DatabaseException("DropTable", entityClassName, "H2",
                        "SQL error during table drop: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                errorHandler.handleError((DatabaseException) e);
                throw e;
            } else {
                DatabaseException dbException = new DatabaseException("DropTable", entityClassName, "H2",
                        "Unexpected error during table drop", e);
                errorHandler.handleError(dbException);
                throw dbException;
            }
        }
    }
}
