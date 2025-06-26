package net.exylia.commons.database.migration;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static net.exylia.commons.utils.DebugUtils.logInfo;
import static net.exylia.commons.utils.DebugUtils.logError;

public class MigrationManager {

    public void createOrUpdateTable(DatabaseAdapter adapter, Class<?> entityClass) throws Exception {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new IllegalArgumentException("La clase debe tener la anotación @Table");
        }

        String version = tableAnnotation.version();
        String tableName = adapter.getTableName(entityClass);

        logInfo("Verificando tabla: " + tableName + " (versión: " + version + ")");

        if (!adapter.tableExists(entityClass)) {
            logInfo("Creando tabla: " + tableName);
            adapter.createTable(entityClass);

            // Guardar información de versión si es SQL
            if (isSQLAdapter(adapter)) {
                createVersionTable(adapter);
                saveTableVersion(adapter, tableName, version);
            }

            logInfo("Tabla creada exitosamente: " + tableName);
        } else {
            // Verificar si necesita actualización
            if (isSQLAdapter(adapter)) {
                String currentVersion = getTableVersion(adapter, tableName);

                logInfo("Versión actual de la tabla " + tableName + ": " + currentVersion +
                        ", Versión esperada: " + version);

                if (!version.equals(currentVersion)) {
                    logInfo("Actualizando tabla " + tableName + " de versión " + currentVersion + " a " + version);
                    adapter.updateTable(entityClass);
                    updateTableVersion(adapter, tableName, version);
                    logInfo("Tabla actualizada exitosamente: " + tableName);
                } else {
                    logInfo("Tabla " + tableName + " ya está en la versión correcta: " + version);
                }
            } else {
                // Para MongoDB, siempre actualizar (es seguro)
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
                // SQL específico para MySQL/MariaDB
                sql = "CREATE TABLE IF NOT EXISTS exylia_migrations (" +
                        "table_name VARCHAR(255) PRIMARY KEY, " +
                        "version VARCHAR(50), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            } else {
                // SQL para H2
                sql = "CREATE TABLE IF NOT EXISTS exylia_migrations (" +
                        "table_name VARCHAR(255) PRIMARY KEY, " +
                        "version VARCHAR(50), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";
            }

            adapter.executeUpdate(sql);
            logInfo("Tabla de migraciones creada/verificada");
        } catch (Exception e) {
            logError("Error creando tabla de migraciones: " + e.getMessage());
        }
    }

    private void saveTableVersion(DatabaseAdapter adapter, String tableName, String version) {
        try {
            if (isMySQLAdapter(adapter)) {
                // Usar INSERT ... ON DUPLICATE KEY UPDATE para MySQL
                String sql = "INSERT INTO exylia_migrations (table_name, version, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE version = VALUES(version), updated_at = CURRENT_TIMESTAMP";
                adapter.executeUpdate(sql, tableName, version);
            } else {
                // Usar MERGE para H2
                String sql = "MERGE INTO exylia_migrations (table_name, version, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP)";
                adapter.executeUpdate(sql, tableName, version);
            }

            logInfo("Versión de tabla guardada: " + tableName + " -> " + version);
        } catch (Exception e) {
            logError("Error guardando versión de tabla: " + e.getMessage());
        }
    }

    private String getTableVersion(DatabaseAdapter adapter, String tableName) {
        try {
            createVersionTable(adapter);
            String sql = "SELECT version FROM exylia_migrations WHERE table_name = ?";

            // Usar una consulta directa para obtener la versión
            if (adapter instanceof net.exylia.commons.database.adapters.H2Adapter ||
                    adapter instanceof net.exylia.commons.database.adapters.MySQLAdapter) {

                try {
                    var results = adapter.executeQuery(Object.class, sql, tableName);
                    if (!results.isEmpty()) {
                        // Extraer la versión del primer resultado
                        Object result = results.get(0);
                        if (result instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
                            Object versionObj = map.get("version");
                            if (versionObj != null) {
                                String foundVersion = versionObj.toString();
                                logInfo("Versión encontrada para tabla " + tableName + ": " + foundVersion);
                                return foundVersion;
                            }
                        }
                    }
                } catch (Exception e) {
                    logInfo("No se encontró versión para tabla " + tableName + ", usando versión por defecto");
                }
            }

            return "1.0"; // Versión por defecto si no se encuentra
        } catch (Exception e) {
            logError("Error obteniendo versión de tabla: " + e.getMessage());
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