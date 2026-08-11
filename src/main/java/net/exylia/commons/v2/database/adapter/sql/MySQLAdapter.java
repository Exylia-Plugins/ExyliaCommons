package net.exylia.commons.v2.database.adapter.sql;

import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MySQLAdapter extends SQLAdapter {

    public MySQLAdapter(AdapterConfig config) {
        super(config);
    }

    @Override
    protected String getJdbcUrl() {
        return config.getJdbcUrl();
    }

    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    protected String getCreateTableSQL(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + metadata.getTableName() + "` (");
        for (FieldDescriptor field : metadata.getFields()) {
            sql.append("`").append(field.getColumnName()).append("` ");
            sql.append(getMySQLType(field));

            if (field.isPrimaryKey()) {
                sql.append(" PRIMARY KEY");
            }
            if (field.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }
            if (!field.isNullable()) {
                sql.append(" NOT NULL");
            }
            if (field.isUnique()) {
                sql.append(" UNIQUE");
            }
            if (!field.getDefaultValue().isEmpty()) {
                sql.append(" DEFAULT ").append(field.getDefaultValue());
            }
            sql.append(",");
        }
        sql.setLength(sql.length() - 1);
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=" + config.getCharset() + " COLLATE=" + config.getCollation());
        return sql.toString();
    }

    @Override
    protected String getInsertSQL(EntityMetadata metadata) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            columns.append("`").append(field.getColumnName()).append("`,");
            values.append("?,");
        }
        columns.setLength(columns.length() - 1);
        values.setLength(values.length() - 1);
        return "INSERT INTO `" + metadata.getTableName() + "`(" + columns + ") VALUES(" + values + ")";
    }

    @Override
    protected String getUpdateSQL(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder("UPDATE `" + metadata.getTableName() + "` SET ");
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isPrimaryKey()) continue;
            sql.append("`").append(field.getColumnName()).append("`=?,");
        }
        sql.setLength(sql.length() - 1);
        sql.append(" WHERE `").append(metadata.getPrimaryKeyField().getColumnName()).append("`=?");
        return sql.toString();
    }

    @Override
    protected String getDeleteSQL(EntityMetadata metadata) {
        return "DELETE FROM `" + metadata.getTableName() + "` WHERE `" + metadata.getPrimaryKeyField().getColumnName() + "`=?";
    }

    @Override
    protected String getSelectByIdSQL(EntityMetadata metadata) {
        return "SELECT * FROM `" + metadata.getTableName() + "` WHERE `" + metadata.getPrimaryKeyField().getColumnName() + "`=?";
    }

    @Override
    protected String getSelectAllSQL(EntityMetadata metadata) {
        return "SELECT * FROM `" + metadata.getTableName() + "`";
    }

    @Override
    protected String getCountSQL(EntityMetadata metadata) {
        return "SELECT COUNT(*) FROM `" + metadata.getTableName() + "`";
    }

    @Override
    public <T extends Entity> void upsertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        if (metadata.getPrimaryKeyField() != null && metadata.getPrimaryKeyField().isAutoIncrement()) {
            super.upsertBatch(entities, metadata);
            return;
        }
        String sql = getUpsertSQL(metadata);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (T entity : entities) {
                    bindInsertValues(stmt, entity, metadata);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private String getUpsertSQL(EntityMetadata metadata) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder updates = new StringBuilder();
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            columns.append("`").append(field.getColumnName()).append("`,");
            values.append("?,");
            if (!field.isPrimaryKey()) {
                updates.append("`").append(field.getColumnName()).append("`=VALUES(`").append(field.getColumnName()).append("`),");
            }
        }
        columns.setLength(columns.length() - 1);
        values.setLength(values.length() - 1);
        updates.setLength(updates.length() - 1);
        return "INSERT INTO `" + metadata.getTableName() + "`(" + columns + ") VALUES(" + values + ") ON DUPLICATE KEY UPDATE " + updates;
    }

    /**
     * MySQL rejects a sub-select that reads the same table a DELETE is writing
     * (error 1093), so the portable sub-select form in {@link SQLAdapter} cannot
     * be used here. It supports {@code DELETE ... ORDER BY ... LIMIT} directly,
     * which is also the cheaper plan: it walks the index on {@code field} and
     * stops at the limit.
     */
    @Override
    public int deleteWhereLessThan(String field, Object value, int limit, EntityMetadata metadata) throws Exception {
        FieldDescriptor target = metadata.getField(field);
        if (target == null) throw new IllegalArgumentException("Field not found: " + field);

        String sql = "DELETE FROM `" + metadata.getTableName() + "`"
                + " WHERE `" + target.getColumnName() + "` < ?"
                + " ORDER BY `" + target.getColumnName() + "` ASC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.setInt(2, limit);
            return stmt.executeUpdate();
        }
    }

    @Override
    public int deleteBounded(int limit, EntityMetadata metadata) throws Exception {
        String sql = "DELETE FROM `" + metadata.getTableName() + "` LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return stmt.executeUpdate();
        }
    }

    @Override
    protected String getModifyColumnSql(String tableName, FieldDescriptor field) {
        return "ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + field.getColumnName() + "` " + getMySQLType(field);
    }

    @Override
    protected String getColumnDefinition(FieldDescriptor field) {
        return getMySQLType(field);
    }

    @Override
    public String getAdapterName() {
        return "MySQL";
    }

    @Override
    protected <T extends Entity> void bindInsertValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            Object value = field.getValue(entity);
            stmt.setObject(index++, convertToSqlValue(value));
        }
    }

    @Override
    protected <T extends Entity> void bindUpdateValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isPrimaryKey()) continue;
            Object value = field.getValue(entity);
            stmt.setObject(index++, convertToSqlValue(value));
        }
        Object idValue = entity.getId() instanceof java.util.UUID ? entity.getId().toString() : entity.getId();
        stmt.setObject(index, idValue);
    }

    private Object convertToSqlValue(Object value) {
        if (value instanceof java.util.UUID) {
            return value.toString();
        }
        return value;
    }

    @Override
    protected <T extends Entity> T mapResultSetToEntity(ResultSet rs, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        for (FieldDescriptor field : metadata.getFields()) {
            Object value = rs.getObject(field.getColumnName());
            if (value != null) {
                field.setValue(entity, value);
            }
        }
        return entity;
    }

    private String getMySQLType(FieldDescriptor field) {
        Class<?> javaType = field.getType();
        int length = field.getLength();

        if (javaType == String.class) {
            if (length == -1) {
                return "LONGTEXT";
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
        if (javaType == boolean.class || javaType == Boolean.class) return "TINYINT(1)";
        return "LONGTEXT";
    }
}
