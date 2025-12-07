package net.exylia.commons.v2.database.adapter.sql;

import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
            sql.append("`").append(field.getColumnName()).append("` ").append(getMySQLType(field.getType()));
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
            if (field.getColumnName().equals("id")) continue;
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
            if (field.isPrimaryKey() || field.getColumnName().equals("id")) continue;
            sql.append("`").append(field.getColumnName()).append("`=?,");
        }
        sql.setLength(sql.length() - 1);
        sql.append(" WHERE `id`=?");
        return sql.toString();
    }

    @Override
    protected String getDeleteSQL(EntityMetadata metadata) {
        return "DELETE FROM `" + metadata.getTableName() + "` WHERE `id`=?";
    }

    @Override
    protected String getSelectByIdSQL(EntityMetadata metadata) {
        return "SELECT * FROM `" + metadata.getTableName() + "` WHERE `id`=?";
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
    public String getAdapterName() {
        return "MySQL";
    }

    @Override
    protected <T extends Entity> void bindInsertValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.getColumnName().equals("id")) continue;
            Object value = field.getValue(entity);
            stmt.setObject(index++, value);
        }
    }

    @Override
    protected <T extends Entity> void bindUpdateValues(PreparedStatement stmt, T entity, EntityMetadata metadata) throws SQLException {
        int index = 1;
        for (FieldDescriptor field : metadata.getFields()) {
            if (field.isPrimaryKey() || field.getColumnName().equals("id")) continue;
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

    private String getMySQLType(Class<?> javaType) {
        if (javaType == String.class) return "VARCHAR(255)";
        if (javaType == int.class || javaType == Integer.class) return "INT";
        if (javaType == long.class || javaType == Long.class) return "BIGINT";
        if (javaType == double.class || javaType == Double.class) return "DOUBLE";
        if (javaType == float.class || javaType == Float.class) return "FLOAT";
        if (javaType == boolean.class || javaType == Boolean.class) return "TINYINT(1)";
        return "LONGTEXT";
    }
}
