package net.exylia.commons.v2.database.adapter.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.utils.DebugUtils;

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
            DebugUtils.logInternalInfo("DataSource already connected for " + getAdapterName());
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
            hikariConfig.setPoolName("ExyliaDB-" + getAdapterName() + "-" + System.currentTimeMillis());

            this.dataSource = new HikariDataSource(hikariConfig);
            DebugUtils.logInternalInfo("Connected to " + getAdapterName());
        } catch (Exception e) {
            DebugUtils.logInternalError("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void disconnect() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            DebugUtils.logInternalInfo("Disconnected from " + getAdapterName());
        }
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public void createTable(EntityMetadata metadata) throws Exception {
        if (!tableExists(metadata)) {
            String sql = getCreateTableSQL(metadata);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                DebugUtils.logInternalInfo("Created table: " + metadata.getTableName());
            }
        }
    }

    @Override
    public void updateTable(EntityMetadata metadata) throws Exception {
        // Implementation for schema migration
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
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindInsertValues(stmt, entity, metadata);
            stmt.executeUpdate();
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
        String sql = fieldName == null ?
            "SELECT * FROM " + metadata.getTableName() + " LIMIT ? OFFSET ?" :
            "SELECT * FROM " + metadata.getTableName() + " WHERE " + fieldName + " = ? LIMIT ? OFFSET ?";

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
    public <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        String order = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM " + metadata.getTableName() + " ORDER BY " + orderByField + " " + order;
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
        String order = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM " + metadata.getTableName() + " ORDER BY " + orderByField + " " + order + " LIMIT ? OFFSET ?";
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
}
