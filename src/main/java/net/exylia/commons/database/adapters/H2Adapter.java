package net.exylia.commons.database.adapters;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class H2Adapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private HikariDataSource dataSource;

    public H2Adapter(FileConfiguration config, ExyliaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public void connect() throws Exception {
        String fileName = config.getString("database.h2.file", "database/h2");
        String username = config.getString("database.h2.username", "sa");
        String password = config.getString("database.h2.password", "");
        int poolSize = config.getInt("database.h2.pool-size", 5);

        String url = "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/" + fileName + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

        // Forzar carga del driver H2
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver H2 no encontrado en el classpath", e);
        }

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

        // Probar conexión
        try (Connection testConnection = dataSource.getConnection()) {
            testConnection.setAutoCommit(true);
            plugin.getLogger().info("Pool de conexiones H2 inicializado exitosamente: " + poolSize + " conexiones");
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public <T> void save(T entity) throws Exception {
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
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            stmt.executeUpdate();
        }
    }

    @Override
    public <T> void saveOrUpdateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String tableName = getTableName(entities.get(0).getClass());
        String primaryKey = getPrimaryKeyField(entities.get(0).getClass());

        // H2 soporta MERGE que es similar a UPSERT
        Map<String, Object> firstEntityMap = entityToMap(entities.get(0));

        StringBuilder sql = new StringBuilder("MERGE INTO " + tableName + " (");
        StringBuilder valuePlaceholders = new StringBuilder("VALUES (");

        List<String> columns = new ArrayList<>();

        // Construir la parte de columnas
        boolean first = true;
        for (String columnName : firstEntityMap.keySet()) {
            if (!first) {
                sql.append(", ");
                valuePlaceholders.append(", ");
            }
            sql.append(columnName);
            valuePlaceholders.append("?");
            columns.add(columnName);
            first = false;
        }

        sql.append(") ").append(valuePlaceholders).append(")");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (T entity : entities) {
                Map<String, Object> entityMap = entityToMap(entity);

                for (int i = 0; i < columns.size(); i++) {
                    stmt.setObject(i + 1, entityMap.get(columns.get(i)));
                }

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            int processed = 0;
            for (int result : results) {
                if (result >= 0) processed++; // MERGE puede retornar diferentes valores
            }

            DebugUtils.logInternalInfo("saveOrUpdateAll completado para " + processed + " de " + entities.size() + " entidades");

        } catch (SQLException e) {
            DebugUtils.logInternalError("Error en saveOrUpdateAll: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public <T> void updateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

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

            for (T entity : entities) {
                Map<String, Object> entityMap = entityToMap(entity);

                int paramIndex = 1;
                // Establecer valores para las columnas de actualización
                for (String column : updateColumns) {
                    stmt.setObject(paramIndex++, entityMap.get(column));
                }
                // Establecer el valor de la clave primaria para el WHERE
                stmt.setObject(paramIndex, entityMap.get(primaryKey));

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            int updated = 0;
            for (int result : results) {
                if (result > 0) updated++;
            }

            DebugUtils.logInternalInfo("updateAll completado: " + updated + " de " + entities.size() + " entidades actualizadas");

        } catch (SQLException e) {
            DebugUtils.logInternalError("Error en updateAll: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public <T> void update(T entity) throws Exception {
        String tableName = getTableName(entity.getClass());
        Map<String, Object> values = entityToMap(entity);

        String primaryKey = getPrimaryKeyField(entity.getClass());
        Object primaryKeyValue = values.get(primaryKey);

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
            stmt.executeUpdate();
        }
    }

    @Override
    public <T> void delete(T entity) throws Exception {
        String tableName = getTableName(entity.getClass());
        String primaryKey = getPrimaryKeyField(entity.getClass());
        Object primaryKeyValue = entityToMap(entity).get(primaryKey);

        String sql = "DELETE FROM " + tableName + " WHERE " + primaryKey + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, primaryKeyValue);
            stmt.executeUpdate();
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityClass, Object id) throws Exception {
        String tableName = getTableName(entityClass);
        String primaryKey = getPrimaryKeyField(entityClass);

        String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKey + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapToEntity(resultSetToMap(rs), entityClass));
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) throws Exception {
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName;

        List<T> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapToEntity(resultSetToMap(rs), entityClass));
            }
        }

        return results;
    }

    @Override
    public <T> List<T> findBy(Class<T> entityClass, String field, Object value) throws Exception {
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName + " WHERE " + field + " = ?";

        List<T> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapToEntity(resultSetToMap(rs), entityClass));
            }
        }

        return results;
    }

    @Override
    public <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception {
        List<T> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapToEntity(resultSetToMap(rs), entityClass));
            }
        }

        return results;
    }

    @Override
    public int executeUpdate(String query, Object... params) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        String tableName = getTableName(entityClass);
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        Field[] fields = entityClass.getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                if (!first) sql.append(", ");

                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                String sqlType = getSQLType(field.getType(), column);

                sql.append(columnName).append(" ").append(sqlType);

                if (column.primaryKey()) {
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

        sql.append(")");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
            DebugUtils.logInternalInfo("Tabla creada: " + tableName);
        }
    }

    @Override
    public void updateTable(Class<?> entityClass) throws Exception {
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

                        // Para nuevas columnas, siempre hacerlas nullable inicialmente
                        // para evitar problemas con datos existentes
                        if (!column.nullable() && !column.primaryKey()) {
                            // Agregar como nullable primero
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(alterSql);
                                DebugUtils.logInternalInfo("Columna agregada (nullable): " + columnName);
                                hasUpdates = true;
                            }

                            // Si tiene valor por defecto, actualizar todos los registros existentes
                            if (!column.defaultValue().isEmpty()) {
                                String updateSql = "UPDATE " + getTableName(entityClass) +
                                        " SET " + columnName + " = ? WHERE " + columnName + " IS NULL";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, column.defaultValue());
                                    int updated = updateStmt.executeUpdate();
                                    DebugUtils.logInternalInfo("Actualizados " + updated + " registros con valor por defecto para " + columnName);
                                }
                            }

                            // Ahora hacer la columna NOT NULL si es necesario
                            String alterNotNullSql = "ALTER TABLE " + getTableName(entityClass) +
                                    " ALTER COLUMN " + columnName + " SET NOT NULL";
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(alterNotNullSql);
                                DebugUtils.logInternalInfo("Columna configurada como NOT NULL: " + columnName);
                            }
                        } else {
                            // Agregar la columna normalmente
                            if (!column.defaultValue().isEmpty()) {
                                alterSql += " DEFAULT '" + column.defaultValue() + "'";
                            }

                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(alterSql);
                                DebugUtils.logInternalInfo("Columna agregada: " + columnName);
                                hasUpdates = true;
                            }
                        }
                    }
                }
            }

            if (hasUpdates) {
                DebugUtils.logInternalInfo("Actualización de tabla completada: " + getTableName(entityClass));
            } else {
                DebugUtils.logInternalInfo("No se requieren actualizaciones para la tabla: " + getTableName(entityClass));
            }
        }
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        String tableName = getTableName(entityClass);
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
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
        }

        return columns;
    }

    @Override
    public void beginTransaction() throws Exception {
        throw new UnsupportedOperationException("Transacciones por pool requieren implementación específica");
    }

    @Override
    public void commit() throws Exception {
        throw new UnsupportedOperationException("Transacciones por pool requieren implementación específica");
    }

    @Override
    public void rollback() throws Exception {
        throw new UnsupportedOperationException("Transacciones por pool requieren implementación específica");
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
        Map<String, Object> map = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                map.put(columnName, field.get(entity));
            }
        }

        return map;
    }

    @Override
    public <T> T mapToEntity(Map<String, Object> map, Class<T> entityClass) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();

                Object value = map.get(columnName);
                if (value != null) {
                    field.set(entity, convertValue(value, field.getType()));
                }
            }
        }

        return entity;
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
        } else {
            return "TEXT";
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.valueOf(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.valueOf(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.valueOf(value.toString());
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.valueOf(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(value.toString());
        }

        return value;
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
}