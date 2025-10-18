package net.exylia.commons.database.migration;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Index;
import net.exylia.commons.database.annotations.Table;

import static net.exylia.commons.utils.DebugUtils.*;

public class MigrationManager {

    public void createOrUpdateTable(DatabaseAdapter adapter, Class<?> entityClass) throws Exception {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new IllegalArgumentException("La clase debe tener la anotación @Table");
        }

        String version = tableAnnotation.version();
        String tableName = adapter.getTableName(entityClass);

        logInternalDebug("Checking table: " + tableName + " (version: " + version + ")");

        if (!adapter.tableExists(entityClass)) {
            logInternalDebug("Creating table: " + tableName);
            adapter.createTable(entityClass);

            if (isSQLAdapter(adapter)) {
                createVersionTable(adapter);
                saveTableVersion(adapter, tableName, version);
                createIndexes(adapter, entityClass, tableAnnotation);
            }

            logInternalDebug("Table created: " + tableName);
        } else {

            if (isSQLAdapter(adapter)) {
                String currentVersion = getTableVersion(adapter, tableName);

                logInternalDebug("Actual table version for " + tableName + ": " + currentVersion +
                        ", Version to check: " + version);

                if (!version.equals(currentVersion)) {
                    logInternalDebug("Updating table " + tableName + " from " + currentVersion + " to " + version);
                    adapter.updateTable(entityClass);
                    updateTableVersion(adapter, tableName, version);
                    createIndexes(adapter, entityClass, tableAnnotation);
                    logInternalDebug("Table updated: " + tableName);
                } else {
                    logInternalDebug("Table " + tableName + " is up to date: " + version);
                }
            } else {

                adapter.updateTable(entityClass);
            }
        }
    }

    private boolean isSQLAdapter(DatabaseAdapter adapter) {
        return adapter.getClass().getSimpleName().contains("SQL") ||
                adapter.getClass().getSimpleName().contains("H2");
    }

    private void createVersionTable(DatabaseAdapter adapter) {
        try {
            String sql;

            if (isMySQLAdapter(adapter)) {
                 
                sql = "CREATE TABLE IF NOT EXISTS exylia_migrations (" +
                        "table_name VARCHAR(255) PRIMARY KEY, " +
                        "version VARCHAR(50), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            } else {
                 
                sql = "CREATE TABLE IF NOT EXISTS exylia_migrations (" +
                        "table_name VARCHAR(255) PRIMARY KEY, " +
                        "version VARCHAR(50), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
            }

            adapter.executeUpdate(sql);
        } catch (Exception e) {
            logInternalError("Error creando tabla de migraciones: " + e.getMessage());
        }
    }

    private void saveTableVersion(DatabaseAdapter adapter, String tableName, String version) {
        try {
            if (isMySQLAdapter(adapter)) {
                 
                String sql = "INSERT INTO exylia_migrations (table_name, version, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE version = VALUES(version), updated_at = CURRENT_TIMESTAMP";
                adapter.executeUpdate(sql, tableName, version);
            } else {
                 
                String sql = "MERGE INTO exylia_migrations (table_name, version, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP)";
                adapter.executeUpdate(sql, tableName, version);
            }

            logInternalDebug("Versión de tabla guardada: " + tableName + " -> " + version);
        } catch (Exception e) {
            logInternalError("Error guardando versión de tabla: " + e.getMessage());
        }
    }

    private String getTableVersion(DatabaseAdapter adapter, String tableName) {
        try {
            createVersionTable(adapter);
            String sql = "SELECT version FROM exylia_migrations WHERE table_name = ?";

            if (adapter instanceof net.exylia.commons.database.adapters.H2Adapter ||
                    adapter instanceof net.exylia.commons.database.adapters.MySQLAdapter) {

                try {
                    var results = adapter.executeQuery(Object.class, sql, tableName);
                    if (!results.isEmpty()) {
                         
                        Object result = results.get(0);
                        if (result instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                            Object versionObj = map.get("version");
                            if (versionObj != null) {
                                String foundVersion = versionObj.toString();
                                logInternalDebug("Versión encontrada para tabla " + tableName + ": " + foundVersion);
                                return foundVersion;
                            }
                        }
                    }
                } catch (Exception e) {
                    logInternalDebug("No se encontró versión para tabla " + tableName + ", usando versión por defecto");
                }
            }

            return "1.0";  
        } catch (Exception e) {
            logInternalError("Error obteniendo versión de tabla: " + e.getMessage());
            return "1.0";
        }
    }

    private void updateTableVersion(DatabaseAdapter adapter, String tableName, String version) {
        saveTableVersion(adapter, tableName, version);
    }

    private boolean isMySQLAdapter(DatabaseAdapter adapter) {
        return adapter.getClass().getSimpleName().contains("MySQL");
    }

    private void createIndexes(DatabaseAdapter adapter, Class<?> entityClass, Table tableAnnotation) {
        try {
            Index[] indexes = tableAnnotation.indexes();
            if (indexes == null || indexes.length == 0) {
                return;
            }

            String tableName = adapter.getTableName(entityClass);

            for (Index index : indexes) {
                try {
                    createIndex(adapter, tableName, index);
                } catch (Exception e) {
                    logInternalError("Failed to create index " + index.name() + " on table " + tableName + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logInternalError("Error processing indexes for table: " + e.getMessage());
        }
    }

    private void createIndex(DatabaseAdapter adapter, String tableName, Index index) throws Exception {
        String indexName = index.name();
        String[] columns = index.columns();
        boolean unique = index.unique();

        if (columns == null || columns.length == 0) {
            logInternalError("Index " + indexName + " has no columns defined");
            return;
        }

        if (indexExists(adapter, tableName, indexName)) {
            logInternalDebug("Index " + indexName + " already exists on table " + tableName);
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");

        if (isMySQLAdapter(adapter)) {
            sql.append("`").append(indexName).append("`");
            sql.append(" ON `").append(tableName).append("` (");

            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append("`").append(columns[i]).append("`");
            }
            sql.append(")");
        } else {
            sql.append(indexName);
            sql.append(" ON ").append(tableName).append(" (");

            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(columns[i]);
            }
            sql.append(")");
        }

        adapter.executeUpdate(sql.toString());
        logInternalInfo("Index created: " + indexName + " on table " + tableName);
    }

    private boolean indexExists(DatabaseAdapter adapter, String tableName, String indexName) {
        try {
            String sql;
            if (isMySQLAdapter(adapter)) {
                sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
            } else {
                sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES " +
                      "WHERE TABLE_NAME = ? AND INDEX_NAME = ?";
            }

            var result = adapter.executeQuery(Object.class, sql, tableName, indexName);
            if (!result.isEmpty() && result.get(0) instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) result.get(0);
                Object count = map.values().iterator().next();
                return count != null && ((Number) count).intValue() > 0;
            }
            return false;
        } catch (Exception e) {
            logInternalDebug("Could not check if index exists: " + e.getMessage());
            return false;
        }
    }
}
