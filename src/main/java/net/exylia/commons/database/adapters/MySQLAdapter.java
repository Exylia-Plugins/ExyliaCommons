package net.exylia.commons.database.adapters;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class MySQLAdapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private HikariDataSource dataSource;

    public MySQLAdapter(FileConfiguration config, ExyliaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public void connect() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();

        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "minecraft");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        boolean ssl = config.getBoolean("database.mysql.ssl", false);
        int poolSize = config.getInt("database.mysql.pool-size", 10);

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC",
                host, port, database, ssl);

        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(60000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        dataSource = new HikariDataSource(hikariConfig);

        // Probar conexión
        try (Connection testConnection = dataSource.getConnection()) {
            testConnection.prepareStatement("SELECT 1").executeQuery();
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
            sql.append("`").append(entry.getKey()).append("`");
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
                sql.append("`").append(entry.getKey()).append("` = ?");
                parameters.add(entry.getValue());
                first = false;
            }
        }

        sql.append(" WHERE `").append(primaryKey).append("` = ?");
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

        String sql = "DELETE FROM " + tableName + " WHERE `" + primaryKey + "` = ?";

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

        String sql = "SELECT * FROM " + tableName + " WHERE `" + primaryKey + "` = ?";

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
        String sql = "SELECT * FROM " + tableName + " WHERE `" + field + "` = ?";

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
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + tableName + "` (");

        Field[] fields = entityClass.getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                if (!first) sql.append(", ");

                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                String sqlType = getMySQLType(field.getType(), column);
                Bukkit.getLogger().info("Usando: " + sqlType + " para " + columnName);

                sql.append("`").append(columnName).append("` ").append(sqlType);

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

        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
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

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();

                if (!existingColumns.contains(columnName.toLowerCase())) {
                    String sqlType = getMySQLType(field.getType(), column);
                    String alterSql = "ALTER TABLE `" + getTableName(entityClass) +
                            "` ADD COLUMN `" + columnName + "` " + sqlType;

                    if (!column.nullable()) {
                        alterSql += " NOT NULL";
                    }

                    if (!column.defaultValue().isEmpty()) {
                        alterSql += " DEFAULT '" + column.defaultValue() + "'";
                    }

                    try (Connection conn = getConnection();
                         Statement stmt = conn.createStatement()) {
                        stmt.execute(alterSql);
                    }
                }
            }
        }
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        String tableName = getTableName(entityClass);
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
        String tableName = getTableName(entityClass);
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";

        List<String> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }

        return columns;
    }

    @Override
    public void beginTransaction() throws Exception {
        // Las transacciones se manejan por conexión en MySQL con HikariCP
        // Esto se implementaría usando ThreadLocal para conexiones por transacción
    }

    @Override
    public void commit() throws Exception {
        // Implementar manejo de transacciones
    }

    @Override
    public void rollback() throws Exception {
        // Implementar manejo de transacciones
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

    private String getMySQLType(Class<?> javaType, Column column) {
        if (javaType == String.class) {
            if (column.length() == -1) {
                return "TEXT";
            }
            if (column.length() > 8000) {
                return "LONGTEXT";
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
            return "DATETIME";
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