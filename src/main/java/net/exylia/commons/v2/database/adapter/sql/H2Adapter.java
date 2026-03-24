package net.exylia.commons.v2.database.adapter.sql;

import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class H2Adapter extends SQLAdapter {

    public H2Adapter(AdapterConfig config) {
        super(config);
    }

    @Override
    public void connect() throws Exception {
        File dbFile = new File(config.getFile());
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        super.connect();
    }

    @Override
    protected String getJdbcUrl() {
        return config.getH2Url();
    }

    @Override
    protected String getDriverClassName() {
        return "org.h2.Driver";
    }

    @Override
    protected String getCreateTableSQL(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + metadata.getTableName() + " (");
        for (FieldDescriptor field : metadata.getFields()) {
            sql.append(field.getColumnName()).append(" ").append(getSQLType(field));
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
        sql.append(")");
        return sql.toString();
    }

    @Override
    protected String getInsertSQL(EntityMetadata metadata) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            columns.append(field.getColumnName()).append(",");
            values.append("?,");
        }
        columns.setLength(columns.length() - 1);
        values.setLength(values.length() - 1);
        return "INSERT INTO " + metadata.getTableName() + "(" + columns + ") VALUES(" + values + ")";
    }

    @Override
    protected String getUpdateSQL(EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder("UPDATE " + metadata.getTableName() + " SET ");
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isPrimaryKey()) continue;
            sql.append(field.getColumnName()).append("=?,");
        }
        sql.setLength(sql.length() - 1);
        sql.append(" WHERE ").append(metadata.getPrimaryKeyField().getColumnName()).append("=?");
        return sql.toString();
    }

    @Override
    protected String getDeleteSQL(EntityMetadata metadata) {
        return "DELETE FROM " + metadata.getTableName() + " WHERE " + metadata.getPrimaryKeyField().getColumnName() + "=?";
    }

    @Override
    protected String getSelectByIdSQL(EntityMetadata metadata) {
        return "SELECT * FROM " + metadata.getTableName() + " WHERE " + metadata.getPrimaryKeyField().getColumnName() + "=?";
    }

    @Override
    protected String getSelectAllSQL(EntityMetadata metadata) {
        return "SELECT * FROM " + metadata.getTableName();
    }

    @Override
    protected String getCountSQL(EntityMetadata metadata) {
        return "SELECT COUNT(*) FROM " + metadata.getTableName();
    }

    @Override
    public <T extends Entity> void upsertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        if (metadata.getPrimaryKeyField() != null && metadata.getPrimaryKeyField().isAutoIncrement()) {
            super.upsertBatch(entities, metadata);
            return;
        }
        String sql = getMergeSQL(metadata);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (T entity : entities) {
                bindInsertValues(stmt, entity, metadata);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private String getMergeSQL(EntityMetadata metadata) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            columns.append(field.getColumnName()).append(",");
            values.append("?,");
        }
        columns.setLength(columns.length() - 1);
        values.setLength(values.length() - 1);
        return "MERGE INTO " + metadata.getTableName() + " (" + columns + ") KEY(" + metadata.getPrimaryKeyField().getColumnName() + ") VALUES(" + values + ")";
    }

    @Override
    protected String getModifyColumnSql(String tableName, FieldDescriptor field) {
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + field.getColumnName() + " " + getSQLType(field);
    }

    @Override
    public String getAdapterName() {
        return "H2";
    }

    @Override
    protected <T extends Entity> void bindInsertValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isAutoIncrement()) continue;
            Object value = field.getValue(entity);
            stmt.setObject(index++, value);
        }
    }

    @Override
    protected <T extends Entity> void bindUpdateValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isPrimaryKey()) continue;
            Object value = field.getValue(entity);
            stmt.setObject(index++, value);
        }
        stmt.setObject(index, entity.getId());
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
}
