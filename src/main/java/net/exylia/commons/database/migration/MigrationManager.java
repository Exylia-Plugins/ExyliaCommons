package net.exylia.commons.database.migration;

import net.exylia.commons.database.adapters.DatabaseAdapter;
import net.exylia.commons.database.annotations.Table;

import static net.exylia.commons.utils.DebugUtils.logInfo;

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

                if (!version.equals(currentVersion)) {
                    logInfo("Actualizando tabla " + tableName + " de versión " + currentVersion + " a " + version);
                    adapter.updateTable(entityClass);
                    updateTableVersion(adapter, tableName, version);
                    logInfo("Tabla actualizada exitosamente: " + tableName);
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
            String sql = "CREATE TABLE IF NOT EXISTS exylia_migrations (" +
                    "table_name VARCHAR(255) PRIMARY KEY, " +
                    "version VARCHAR(50), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            adapter.executeUpdate(sql);
        } catch (Exception e) {
            // Ignorar si ya existe
        }
    }

    private void saveTableVersion(DatabaseAdapter adapter, String tableName, String version) {
        try {
            String sql = "INSERT INTO exylia_migrations (table_name, version) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE version = ?, updated_at = CURRENT_TIMESTAMP";
            adapter.executeUpdate(sql, tableName, version, version);
        } catch (Exception e) {
            // Fallback para H2
            try {
                String sql = "MERGE INTO exylia_migrations (table_name, version) VALUES (?, ?)";
                adapter.executeUpdate(sql, tableName, version);
            } catch (Exception fallbackError) {
                logInfo("No se pudo guardar la versión de la tabla: " + fallbackError.getMessage());
            }
        }
    }

    private String getTableVersion(DatabaseAdapter adapter, String tableName) {
        try {
            createVersionTable(adapter);
            // Implementar consulta para obtener versión
            return "1.0"; // Por ahora retornar versión por defecto
        } catch (Exception e) {
            return "1.0";
        }
    }

    private void updateTableVersion(DatabaseAdapter adapter, String tableName, String version) {
        saveTableVersion(adapter, tableName, version);
    }
}