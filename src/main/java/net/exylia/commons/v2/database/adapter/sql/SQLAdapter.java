package net.exylia.commons.v2.database.adapter.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.v2.debug.api.DebugAPI;

import net.exylia.commons.v2.database.annotation.Index;

import java.sql.*;
import java.util.*;

public abstract class SQLAdapter implements DatabaseAdapter {

    protected HikariDataSource dataSource;
    protected final AdapterConfig config;

    public SQLAdapter(AdapterConfig config) {
        this.config = config;
    }

    protected abstract String getJdbcUrl();

    protected abstract String getDriverClassName();

    protected abstract String getCreateTableSQL(EntityMetadata metadata);

    protected abstract String getInsertSQL(EntityMetadata metadata);

    protected abstract String getUpdateSQL(EntityMetadata metadata);

    protected abstract String getDeleteSQL(EntityMetadata metadata);

    protected abstract String getSelectByIdSQL(EntityMetadata metadata);

    protected abstract String getSelectAllSQL(EntityMetadata metadata);

    protected abstract String getCountSQL(EntityMetadata metadata);

    @Override
    public void connect() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            DebugAPI.logLibInfo("DataSource already connected for " + getAdapterName());
            return;
        }

        try {
            Class.forName(getDriverClassName());

            String jdbcUrl = getJdbcUrl();
            String username = config.getUsername();
            String password = config.getPassword();
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(config.getPoolSize());
            hikariConfig.setMinimumIdle(config.getMinIdle());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
            hikariConfig.setIdleTimeout(config.getIdleTimeoutMs());
            hikariConfig.setMaxLifetime(config.getMaxLifetimeMs());
            hikariConfig.setAutoCommit(true);
            hikariConfig.setKeepaliveTime(30_000);
            hikariConfig.setValidationTimeout(3_000);
            hikariConfig.setPoolName("ExyliaDB-" + getAdapterName() + "-" + System.currentTimeMillis());

            this.dataSource = new HikariDataSource(hikariConfig);
            DebugAPI.logLibInfo("Connected to " + getAdapterName());
        } catch (Exception e) {
            DebugAPI.logLibError("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void disconnect() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            DebugAPI.logLibInfo("Disconnected from " + getAdapterName());
        }
    }

    @Override
    public void reconnect() throws Exception {
        DebugAPI.logLibInfo("Reconnecting to " + getAdapterName() + "...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
        connect();
    }

    @Override
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createTable(EntityMetadata metadata) throws Exception {
        if (!tableExists(metadata)) {
            String sql = getCreateTableSQL(metadata);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                DebugAPI.logLibInfo("Created table: " + metadata.getTableName());
            }
        }
        createIndexes(metadata);
    }

    private void createIndexes(EntityMetadata metadata) {
        if (metadata.getIndexes().isEmpty()) return;
        try (Connection conn = dataSource.getConnection()) {
            for (Index index : metadata.getIndexes()) {
                String columns = String.join(", ", index.fields());
                String sql = "CREATE INDEX " + index.name() + " ON " + metadata.getTableName() + " (" + columns + ")";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            DebugAPI.logLibError("Failed to create indexes for " + metadata.getTableName() + ": " + e.getMessage());
        }
    }

    @Override
    public void updateTable(EntityMetadata metadata) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Map<String, Integer> existingColumns = getExistingColumnSizes(conn, metadata.getTableName());

            for (FieldDescriptor field : metadata.getFields()) {
                String columnName = field.getColumnName().toLowerCase();
                if (!existingColumns.containsKey(columnName)) {
                    String alterSql = "ALTER TABLE " + metadata.getTableName() +
                        " ADD COLUMN " + field.getColumnName() + " " + getSQLType(field);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(alterSql);
                        DebugAPI.logLibInfo("Added column " + field.getColumnName() + " to " + metadata.getTableName());
                    }
                } else if (field.isString() && field.getLength() > 0) {
                    int currentSize = existingColumns.get(columnName);
                    if (currentSize < field.getLength()) {
                        String modifySql = getModifyColumnSql(metadata.getTableName(), field);
                        if (modifySql != null) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(modifySql);
                                DebugAPI.logLibInfo("Resized column " + field.getColumnName() + " in " + metadata.getTableName() + " from " + currentSize + " to " + field.getLength());
                            }
                        }
                    }
                }
            }
        }
    }

    protected String getModifyColumnSql(String tableName, FieldDescriptor field) {
        return null;
    }

    private Map<String, Integer> getExistingColumnSizes(Connection conn, String tableName) throws SQLException {
        Map<String, Integer> columns = new HashMap<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getInt("COLUMN_SIZE"));
            }
        }
        if (columns.isEmpty()) {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), null)) {
                while (rs.next()) {
                    columns.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getInt("COLUMN_SIZE"));
                }
            }
        }
        return columns;
    }

    @Override
    public boolean tableExists(EntityMetadata metadata) throws Exception {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, metadata.getTableName(), null)) {
            return rs.next();
        }
    }

    @Override
    public <T extends Entity> void insert(T entity, EntityMetadata metadata) throws Exception {
        String sql = getInsertSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsertValues(stmt, entity, metadata);
            stmt.executeUpdate();

            FieldDescriptor primaryKey = metadata.getPrimaryKeyField();
            if (primaryKey != null && primaryKey.isAutoIncrement()) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Object generatedId = generatedKeys.getObject(1);
                        if (generatedId != null) {
                            primaryKey.getField().setAccessible(true);

                            if (primaryKey.getField().getType() == long.class || primaryKey.getField().getType() == Long.class) {
                                primaryKey.getField().set(entity, ((Number) generatedId).longValue());
                            } else if (primaryKey.getField().getType() == int.class || primaryKey.getField().getType() == Integer.class) {
                                primaryKey.getField().set(entity, ((Number) generatedId).intValue());
                            } else {
                                primaryKey.getField().set(entity, generatedId);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public <T extends Entity> void update(T entity, EntityMetadata metadata) throws Exception {
        String sql = getUpdateSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindUpdateValues(stmt, entity, metadata);
            stmt.executeUpdate();
        }
    }

    @Override
    public <T extends Entity> void delete(T entity, EntityMetadata metadata) throws Exception {
        String sql = getDeleteSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            Object idValue = entity.getId() instanceof java.util.UUID ? entity.getId().toString() : entity.getId();
            stmt.setObject(1, idValue);
            stmt.executeUpdate();
        }
    }

    @Override
    public <T extends Entity> Optional<T> findById(Object id, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        String sql = getSelectByIdSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            Object idValue = id instanceof java.util.UUID ? id.toString() : id;
            stmt.setObject(1, idValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEntity(rs, entityClass, metadata));
            }
            return Optional.empty();
        }
    }

    @Override
    public <T extends Entity> List<T> findAll(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        String sql = getSelectAllSQL(metadata);
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        FieldDescriptor field = metadata.getField(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }

        String sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + field.getColumnName() + " = ?";
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> long count(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        String sql = getCountSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    @Override
    public <T extends Entity> long countByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        FieldDescriptor field = metadata.getField(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }

        String sql = "SELECT COUNT(*) FROM " + metadata.getTableName() + " WHERE " + field.getColumnName() + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    @Override
    public <T extends Entity> List<T> executeQuery(String query, List<Object> params, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByFieldPaged(String fieldName, Object value, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        List<T> results = new ArrayList<>();
        String sql;
        if (fieldName == null) {
            sql = "SELECT * FROM " + metadata.getTableName() + " LIMIT ? OFFSET ?";
        } else {
            FieldDescriptor field = metadata.getField(fieldName);
            if (field == null) throw new IllegalArgumentException("Field not found: " + fieldName);
            sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + field.getColumnName() + " = ? LIMIT ? OFFSET ?";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int offset = page * pageSize;
            if (fieldName == null) {
                stmt.setInt(1, pageSize);
                stmt.setInt(2, offset);
            } else {
                stmt.setObject(1, value);
                stmt.setInt(2, pageSize);
                stmt.setInt(3, offset);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> void insertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        String sql = getInsertSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (T entity : entities) {
                bindInsertValues(stmt, entity, metadata);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public <T extends Entity> void updateBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        String sql = getUpdateSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (T entity : entities) {
                bindUpdateValues(stmt, entity, metadata);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public <T extends Entity> void upsertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        for (T entity : entities) {
            if (entity.getId() != null && findById(entity.getId(), entity.getClass(), metadata).isPresent()) {
                update(entity, metadata);
            } else {
                insert(entity, metadata);
            }
        }
    }

    @Override
    public <T extends Entity> void deleteBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        String sql = getDeleteSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (T entity : entities) {
                Object idValue = entity.getId() instanceof java.util.UUID ? entity.getId().toString() : entity.getId();
                stmt.setObject(1, idValue);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public int truncate(EntityMetadata metadata) throws Exception {
        String sql = "DELETE FROM " + metadata.getTableName();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * Portable form: bound the victims with a sub-select on the primary key.
     * {@code DELETE ... LIMIT} is a vendor extension rather than standard SQL, so
     * the base implementation avoids it; MySQL overrides this with the cheaper
     * direct form.
     */
    @Override
    public int deleteWhereLessThan(String field, Object value, int limit, EntityMetadata metadata) throws Exception {
        FieldDescriptor target = metadata.getField(field);
        if (target == null) throw new IllegalArgumentException("Field not found: " + field);
        FieldDescriptor pk = metadata.getPrimaryKeyField();
        if (pk == null) throw new IllegalStateException("Bounded delete requires a primary key on " + metadata.getTableName());

        String table = metadata.getTableName();
        String sql = "DELETE FROM " + table + " WHERE " + pk.getColumnName() + " IN ("
                + "SELECT " + pk.getColumnName() + " FROM " + table
                + " WHERE " + target.getColumnName() + " < ?"
                + " ORDER BY " + target.getColumnName() + " ASC LIMIT ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.setInt(2, limit);
            return stmt.executeUpdate();
        }
    }

    @Override
    public int deleteBounded(int limit, EntityMetadata metadata) throws Exception {
        FieldDescriptor pk = metadata.getPrimaryKeyField();
        if (pk == null) throw new IllegalStateException("Bounded delete requires a primary key on " + metadata.getTableName());

        String table = metadata.getTableName();
        String sql = "DELETE FROM " + table + " WHERE " + pk.getColumnName() + " IN ("
                + "SELECT " + pk.getColumnName() + " FROM " + table + " LIMIT ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return stmt.executeUpdate();
        }
    }

    @Override
    public <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        FieldDescriptor orderField = metadata.getField(orderByField);
        if (orderField == null) throw new IllegalArgumentException("Field not found: " + orderByField);
        String order = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM " + metadata.getTableName() + " ORDER BY " + orderField.getColumnName() + " " + order;
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findAllSortedPaged(String orderByField, boolean ascending, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        FieldDescriptor orderField = metadata.getField(orderByField);
        if (orderField == null) throw new IllegalArgumentException("Field not found: " + orderByField);
        String order = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM " + metadata.getTableName() + " ORDER BY " + orderField.getColumnName() + " " + order + " LIMIT ? OFFSET ?";
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, page * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByFieldSorted(String whereField, Object whereValue, String orderField, boolean ascending, int limit, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        FieldDescriptor field = metadata.getField(whereField);
        if (field == null) throw new IllegalArgumentException("Field not found: " + whereField);
        FieldDescriptor sortField = metadata.getField(orderField);
        if (sortField == null) throw new IllegalArgumentException("Field not found: " + orderField);
        String order = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + field.getColumnName() + " = ? ORDER BY " + sortField.getColumnName() + " " + order + " LIMIT ?";
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, whereValue);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs, entityClass, metadata));
            }
        }
        return results;
    }

    @Override
    public boolean supportsBatchOperations() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    protected abstract <T extends Entity> void bindInsertValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException;

    protected abstract <T extends Entity> void bindUpdateValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException;

    protected abstract <T extends Entity> T mapResultSetToEntity(ResultSet rs, Class<T> entityClass, EntityMetadata metadata) throws Exception;

    protected String getSQLType(Class<?> javaType) {
        if (javaType == String.class) return "VARCHAR(255)";
        if (javaType == int.class || javaType == Integer.class) return "INT";
        if (javaType == long.class || javaType == Long.class) return "BIGINT";
        if (javaType == double.class || javaType == Double.class) return "DOUBLE";
        if (javaType == float.class || javaType == Float.class) return "FLOAT";
        if (javaType == boolean.class || javaType == Boolean.class) return "BOOLEAN";
        return "TEXT";
    }

    protected String getSQLType(FieldDescriptor field) {
        Class<?> javaType = field.getType();
        int length = field.getLength();

        if (javaType == String.class) {
            if (length == -1) {
                return "TEXT";
            } else if (length > 0) {
                return "VARCHAR(" + length + ")";
            }
            return "VARCHAR(255)";
        }
        if (javaType == java.util.UUID.class) return "VARCHAR(36)";
        if (javaType == int.class || javaType == Integer.class) return "INT";
        if (javaType == long.class || javaType == Long.class) return "BIGINT";
        if (javaType == double.class || javaType == Double.class) return "DOUBLE";
        if (javaType == float.class || javaType == Float.class) return "FLOAT";
        if (javaType == boolean.class || javaType == Boolean.class) return "BOOLEAN";
        return "TEXT";
    }
}
