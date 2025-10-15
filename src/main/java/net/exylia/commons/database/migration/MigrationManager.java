package net.exylia.commons.database.migration;

import net.exylia.commons.database.adapters.DatabaseAdapter;
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
}
