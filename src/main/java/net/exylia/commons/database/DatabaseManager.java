package net.exylia.commons.database;

import lombok.Getter;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final ExyliaPlugin plugin;
    @Getter
    private DatabaseAdapter adapter;
    private final ExecutorService executor;
    private final Map<Class<?>, Repository<?>> repositories;
    private final Set<Class<?>> registeredEntities;
    private final MigrationManager migrationManager;
    private FileConfiguration databaseConfig;
    private volatile boolean tablesInitialized = false;
    private final Object initializationLock = new Object();

    private DatabaseManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(4);
        this.repositories = new HashMap<>();
        this.registeredEntities = new HashSet<>();
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

        } catch (Exception e) {
            plugin.getLogger().severe("Error conectando a la base de datos: " + e.getMessage());
            if (!type.equals("H2")) {
                try {
                    adapter = new H2Adapter(databaseConfig, plugin);
                    adapter.connect();
                } catch (Exception fallbackError) {
                    plugin.getLogger().severe("Error en fallback a H2: " + fallbackError.getMessage());
                }
            }
        }
    }

    public <T> void registerEntity(Class<T> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("La clase " + entityClass.getName() + " debe tener la anotación @Table");
        }

        registeredEntities.add(entityClass);
        logInternalDebug(debug(), "Entidad registrada para inicialización: " + entityClass.getSimpleName());
    }

    public CompletableFuture<Void> initializeAllTables() {
        return CompletableFuture.runAsync(() -> {
            synchronized (initializationLock) {
                if (tablesInitialized) {
                    logInternalDebug(debug(), "Tablas ya inicializadas, omitiendo...");
                    return;
                }

                try {
                    logInternalDebug(debug(), "=== INICIANDO INICIALIZACIÓN DE TABLAS ===");

                    if (databaseConfig.getBoolean("database.auto-migrate", true)) {
                        for (Class<?> entityClass : registeredEntities) {
                            try {
                                logInternalDebug(debug(), "Inicializando tabla para: " + entityClass.getSimpleName());
                                migrationManager.createOrUpdateTable(adapter, entityClass);
                                logInternalDebug(debug(), "✅ Tabla inicializada: " + entityClass.getSimpleName());
                            } catch (Exception e) {
                                logInternalError("❌ Error inicializando tabla " + entityClass.getSimpleName() + ": " + e.getMessage());
                                throw new RuntimeException("Error inicializando tabla " + entityClass.getSimpleName(), e);
                            }
                        }
                    }

                    tablesInitialized = true;
                    logInternalDebug(debug(), "=== INICIALIZACIÓN DE TABLAS COMPLETADA ===");

                } catch (Exception e) {
                    logInternalError("Error durante inicialización de tablas: " + e.getMessage());
                    throw new RuntimeException("Error durante inicialización de tablas", e);
                }
            }
        }, executor);
    }

    public void waitForTablesInitialization() {
        waitForTablesInitialization(30); // 30 segundos timeout
    }

    public boolean waitForTablesInitialization(int timeoutSeconds) {
        synchronized (initializationLock) {
            if (tablesInitialized) {
                return true;
            }
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (!tablesInitialized && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(100); // Check cada 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        if (!tablesInitialized) {
            logInternalError("Timeout esperando inicialización de tablas (" + timeoutSeconds + "s)");
            return false;
        }

        logInternalDebug(debug(), "Tablas inicializadas correctamente");
        return true;
    }

    public boolean areTablesReady() {
        return tablesInitialized;
    }


    public void reregisterAllEntities() {
        CompletableFuture.runAsync(() -> {
            synchronized (initializationLock) {
                for (Class<?> entityClass : registeredEntities) {
                    try {
                        if (databaseConfig.getBoolean("database.auto-migrate", true)) {
                            migrationManager.createOrUpdateTable(adapter, entityClass);
                        }
                    } catch (Exception e) {
                        logInternalError("Error re-registrando entidad " + entityClass.getName() + ": " + e.getMessage());
                    }
                }
            }
        }, executor);
    }

    @SuppressWarnings("unchecked")
    public <T> Repository<T> getRepository(Class<T> entityClass) {
        if (!areTablesReady()) {
            logInternalError("Intento de obtener repositorio antes de inicialización de tablas: " + entityClass.getSimpleName());
            if (!waitForTablesInitialization(5)) {
                throw new IllegalStateException("Tablas no están inicializadas para " + entityClass.getSimpleName());
            }
        }

        return (Repository<T>) repositories.computeIfAbsent(entityClass, clazz -> {
            try {
                Constructor<RepositoryImpl> constructor = RepositoryImpl.class.getConstructor(
                        DatabaseAdapter.class, Class.class, ExecutorService.class
                );
                return constructor.newInstance(adapter, clazz, executor);
            } catch (Exception e) {
                logInternalError("Error creando repositorio para " + clazz.getName() + ": " + e.getMessage());
                throw new RuntimeException("Error creando repositorio para " + clazz.getName(), e);
            }
        });
    }

    public void recreateAllRepositories() {
        Set<Class<?>> entityClasses = new HashSet<>(repositories.keySet());
        repositories.clear();
        for (Class<?> entityClass : entityClasses) {
            try {
                Constructor<RepositoryImpl> constructor = RepositoryImpl.class.getConstructor(
                        DatabaseAdapter.class, Class.class, ExecutorService.class
                );
                Repository<?> newRepository = constructor.newInstance(adapter, entityClass, executor);
                repositories.put(entityClass, newRepository);

            } catch (Exception e) {
                logInternalError("Error recreando repositorio para " + entityClass.getName() + ": " + e.getMessage());
            }
        }

        logInternalDebug(debug(), "Recreación de repositorios completada");
    }

    public boolean performCompleteReload() {
        try {
            logInternalDebug(debug(), "=== INICIANDO RELOAD COMPLETO ===");

            // 1. Resetear estado de inicialización
            synchronized (initializationLock) {
                tablesInitialized = false;
            }

            // 2. Reconectar
            logInternalDebug(debug(), "Reconnecting to database...");
            reconnect();

            if (!isConnected()) {
                logInternalError("Error: No se pudo establecer conexión a la base de datos");
                return false;
            }

            // 3. Inicializar tablas de forma sincronizada
            logInternalDebug(debug(), "Initializing tables synchronously...");
            CompletableFuture<Void> initFuture = initializeAllTables();

            // Esperar a que termine la inicialización (máximo 30 segundos)
            try {
                initFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                logInternalError("Error durante inicialización de tablas en reload: " + e.getMessage());
                return false;
            }

            // 4. Recrear repositorios
            logInternalDebug(debug(), "Recreating all repositories...");
            recreateAllRepositories();

            logInternalSuccess("=== RELOAD COMPLETO EXITOSO ===");
            return true;

        } catch (Exception e) {
            logInternalError("Error durante reload completo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void clearRepositoryCache() {
        logInternalDebug(debug(), "Cleaning repository cache...");
        repositories.clear();
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

    /**
     * Reconecta a la base de datos
     */
    public void reconnect() {
        try {
            if (adapter != null) {
                adapter.disconnect();
            }
            Thread.sleep(500);
            loadConfiguration();
            connectToDatabase();
        } catch (Exception e) {
            logInternalError("Error durante reconexión: " + e.getMessage());
            throw new RuntimeException("Error durante reconexión", e);
        }
    }

    public boolean isConnected() {
        return adapter != null && adapter.isConnected();
    }

    public Set<Class<?>> getRegisteredEntities() {
        return new HashSet<>(registeredEntities);
    }


    public void shutdown() {
        logInternalDebug(debug(), "Closing database connections...");

        synchronized (initializationLock) {
            tablesInitialized = false;
        }

        clearRepositoryCache();
        registeredEntities.clear();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
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