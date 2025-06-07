package net.exylia.commons.database;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.adapters.*;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.migration.MigrationManager;
import net.exylia.commons.database.repository.Repository;
import net.exylia.commons.database.repository.RepositoryImpl;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.exylia.commons.utils.DebugUtils.logInfo;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final ExyliaPlugin plugin;
    private DatabaseAdapter adapter;
    private final ExecutorService executor;
    private final Map<Class<?>, Repository<?>> repositories;
    private final MigrationManager migrationManager;
    private FileConfiguration databaseConfig;

    private DatabaseManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(4);
        this.repositories = new HashMap<>();
        this.migrationManager = new MigrationManager();
    }

    public static void initialize(ExyliaPlugin plugin) {
        if (instance == null) {
            instance = new DatabaseManager(plugin);
            instance.loadConfiguration();
            instance.connectToDatabase();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager no ha sido inicializado");
        }
        return instance;
    }

    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "database.yml");

        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        databaseConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Configuración por defecto con H2
            config.set("database.type", "H2");
            config.set("database.h2.file", "database/h2");
            config.set("database.h2.username", "sa");
            config.set("database.h2.password", "");
            config.set("database.h2.pool-size", 5);

            // Configuración MySQL/MariaDB optimizada
            config.set("database.mysql.host", "localhost");
            config.set("database.mysql.port", 3306);
            config.set("database.mysql.database", "minecraft");
            config.set("database.mysql.username", "root");
            config.set("database.mysql.password", "password");
            config.set("database.mysql.ssl", false);
            config.set("database.mysql.pool-size", 10);
            config.set("database.mysql.minimum-idle", 2);
            config.set("database.mysql.connection-timeout", 30000);
            config.set("database.mysql.idle-timeout", 600000);
            config.set("database.mysql.max-lifetime", 1800000);

            // Configuración MongoDB
            config.set("database.mongodb.host", "localhost");
            config.set("database.mongodb.port", 27017);
            config.set("database.mongodb.database", "minecraft");
            config.set("database.mongodb.username", "");
            config.set("database.mongodb.password", "");
            config.set("database.mongodb.auth-database", "admin");
            config.set("database.mongodb.connection-pool-size", 10);

            // Configuración general
            config.set("database.auto-migrate", true);
            config.set("database.debug", false);
            config.set("database.enable-metrics", false);

            config.save(configFile);
            logInfo("Archivo database.yml creado con configuración optimizada");

        } catch (IOException e) {
            plugin.getLogger().severe("Error creando archivo database.yml: " + e.getMessage());
        }
    }

    private void connectToDatabase() {
        String type = databaseConfig.getString("database.type", "H2").toUpperCase();

        try {
            switch (type) {
                case "H2":
                    adapter = new H2Adapter(databaseConfig, plugin);
                    break;
                case "MYSQL":
                case "MARIADB":
                    adapter = new MySQLAdapter(databaseConfig, plugin);
                    break;
                case "MONGODB":
                    adapter = new MongoDBAdapter(databaseConfig, plugin);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de base de datos no soportado: " + type);
            }

            adapter.connect();
            logInfo("Conectado a la base de datos: " + type);

        } catch (Exception e) {
            plugin.getLogger().severe("Error conectando a la base de datos: " + e.getMessage());
            // Fallback a H2 si falla la conexión principal
            if (!type.equals("H2")) {
                logInfo("Intentando fallback a H2...");
                try {
                    adapter = new H2Adapter(databaseConfig, plugin);
                    adapter.connect();
                    logInfo("Fallback a H2 exitoso");
                } catch (Exception fallbackError) {
                    plugin.getLogger().severe("Error en fallback a H2: " + fallbackError.getMessage());
                }
            }
        }
    }

    /**
     * Registra una entidad para crear su tabla automáticamente
     */
    public <T> void registerEntity(Class<T> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("La clase " + entityClass.getName() + " debe tener la anotación @Table");
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (databaseConfig.getBoolean("database.auto-migrate", true)) {
                    migrationManager.createOrUpdateTable(adapter, entityClass);
                }
                logInfo("Entidad registrada: " + entityClass.getSimpleName());
            } catch (Exception e) {
                plugin.getLogger().severe("Error registrando entidad " + entityClass.getName() + ": " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Obtiene un repositorio para una entidad
     */
    @SuppressWarnings("unchecked")
    public <T> Repository<T> getRepository(Class<T> entityClass) {
        return (Repository<T>) repositories.computeIfAbsent(entityClass, clazz -> {
            try {
                Constructor<RepositoryImpl> constructor = RepositoryImpl.class.getConstructor(
                        DatabaseAdapter.class, Class.class, ExecutorService.class
                );
                return constructor.newInstance(adapter, clazz, executor);
            } catch (Exception e) {
                throw new RuntimeException("Error creando repositorio para " + clazz.getName(), e);
            }
        });
    }

    /**
     * Ejecuta una operación asíncrona en la base de datos
     */
    public <T> CompletableFuture<T> executeAsync(DatabaseOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.execute(adapter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * Ejecuta una operación síncrona en la base de datos
     */
    public <T> T executeSync(DatabaseOperation<T> operation) {
        try {
            return operation.execute(adapter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseAdapter getAdapter() {
        return adapter;
    }

    public boolean isConnected() {
        return adapter != null && adapter.isConnected();
    }

    public void shutdown() {
        logInfo("Cerrando conexiones de base de datos...");

        repositories.clear();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        if (adapter != null) {
            adapter.disconnect();
        }

        instance = null;
    }

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(DatabaseAdapter adapter) throws Exception;
    }
}