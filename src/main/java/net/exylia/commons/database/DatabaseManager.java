package net.exylia.commons.database;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.adapters.*;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.exceptions.*;
import net.exylia.commons.database.io.*;
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
    @Getter
    private DatabaseExportImportManager exportImportManager;

    @Getter
    private DatabaseErrorHandler errorHandler;

    private DatabaseManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(4);
        this.repositories = new HashMap<>();
        this.registeredEntities = new HashSet<>();
        this.migrationManager = new MigrationManager();
        this.errorHandler = new DatabaseErrorHandler(plugin);
        this.exportImportManager = new DatabaseExportImportManager(plugin, this);
    }

    public static void initialize(ExyliaPlugin plugin) {
        if (instance == null) {
            instance = new DatabaseManager(plugin);
            try {
                instance.loadConfiguration();
                instance.connectToDatabase();
            } catch (Exception e) {
                instance.errorHandler.handleGenericError("Initialization", "DatabaseManager", "System", e);
                throw new DatabaseException("Initialization", "DatabaseManager", "System",
                        "Failed to initialize database manager", e);
            }
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            return null;
        }
        return instance;
    }

    private void loadConfiguration() {
        try {
            File configFile = new File(plugin.getDataFolder(), "database.yml");

            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }

            databaseConfig = YamlConfiguration.loadConfiguration(configFile);
            logInternalInfo("Database configuration loaded successfully");

        } catch (Exception e) {
            throw new DatabaseException("Configuration Loading", "DatabaseManager", "System",
                    "Failed to load database configuration", e);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            config.set("database.type", "H2");

            config.set("database.yaml.directory", "data");
            config.set("database.yaml.auto-save", true);
            config.set("database.yaml.backup-on-shutdown", true);

            config.set("database.h2.file", "database/h2");
            config.set("database.h2.username", "sa");
            config.set("database.h2.password", "");
            config.set("database.h2.pool-size", 5);

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

            config.set("database.mongodb.host", "localhost");
            config.set("database.mongodb.port", 27017);
            config.set("database.mongodb.database", "minecraft");
            config.set("database.mongodb.username", "");
            config.set("database.mongodb.password", "");
            config.set("database.mongodb.auth-database", "admin");
            config.set("database.mongodb.connection-pool-size", 10);

            config.set("database.auto-migrate", true);
            config.set("database.debug", false);
            config.set("database.enable-metrics", false);

            config.save(configFile);
            logInternalInfo("Default database configuration created with YAML support");

        } catch (IOException e) {
            throw new DatabaseException("Configuration Creation", "DatabaseManager", "System",
                    "Failed to create default database configuration file", e);
        }
    }

    private void connectToDatabase() {
        String type = databaseConfig.getString("database.type", "YAML").toUpperCase();
        String connectionInfo = null;

        try {
            switch (type) {
                case "YAML":
                    String directory = databaseConfig.getString("database.yaml.directory", "data");
                    connectionInfo = "YAML directory: " + directory;
                    adapter = new YAMLAdapter(databaseConfig, plugin, errorHandler);
                    break;
                case "H2":
                    String fileName = databaseConfig.getString("database.h2.file", "database/h2");
                    connectionInfo = "H2 file: " + fileName;
                    adapter = new H2Adapter(databaseConfig, plugin, errorHandler);
                    break;
                case "MYSQL":
                case "MARIADB":
                    String host = databaseConfig.getString("database.mysql.host", "localhost");
                    int port = databaseConfig.getInt("database.mysql.port", 3306);
                    String database = databaseConfig.getString("database.mysql.database", "minecraft");
                    connectionInfo = String.format("MySQL: %s:%d/%s", host, port, database);
                    adapter = new MySQLAdapter(databaseConfig, plugin, errorHandler);
                    break;
                case "MONGODB":
                    String mongoHost = databaseConfig.getString("database.mongodb.host", "localhost");
                    int mongoPort = databaseConfig.getInt("database.mongodb.port", 27017);
                    String mongoDb = databaseConfig.getString("database.mongodb.database", "minecraft");
                    connectionInfo = String.format("MongoDB: %s:%d/%s", mongoHost, mongoPort, mongoDb);
                    adapter = new MongoDBAdapter(databaseConfig, plugin, errorHandler);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported database type: " + type);
            }

            logInternalInfo("Attempting to connect to database: " + connectionInfo);
            adapter.connect();
            logInternalSuccess("Successfully connected to " + type + " database");

        } catch (Exception e) {
            String errorMsg = String.format("Failed to connect to %s database (%s)", type, connectionInfo);

            if (!type.equals("YAML") && !type.equals("H2")) {
                errorHandler.logWarning("Connection", type, "Primary database connection failed, attempting YAML fallback");

                try {
                    adapter = new YAMLAdapter(databaseConfig, plugin, errorHandler);
                    adapter.connect();
                    errorHandler.logRecovery("Connection", "YAML", "Successfully fell back to YAML database");

                } catch (Exception yamlError) {
                    errorHandler.logWarning("Connection", "YAML", "YAML fallback failed, attempting H2 fallback");

                    try {
                        adapter = new H2Adapter(databaseConfig, plugin, errorHandler);
                        adapter.connect();
                        errorHandler.logRecovery("Connection", "H2", "Successfully fell back to H2 database");

                    } catch (Exception fallbackError) {
                        throw new ConnectionException("H2", "fallback",
                                "Primary database, YAML fallback, and H2 fallback all failed", fallbackError);
                    }
                }
            } else if (type.equals("YAML")) {
                errorHandler.logWarning("Connection", "YAML", "YAML connection failed, attempting H2 fallback");

                try {
                    adapter = new H2Adapter(databaseConfig, plugin, errorHandler);
                    adapter.connect();
                    errorHandler.logRecovery("Connection", "H2", "Successfully fell back to H2 database");

                } catch (Exception fallbackError) {
                    throw new ConnectionException("H2", "fallback",
                            "Both YAML and H2 fallback failed", fallbackError);
                }
            } else {
                throw new ConnectionException(type, connectionInfo, errorMsg, e);
            }
        }
    }

    public <T> void registerEntity(Class<T> entityClass) {
        try {
            if (!entityClass.isAnnotationPresent(Table.class)) {
                throw new IllegalArgumentException("Class " + entityClass.getName() + " must have @Table annotation");
            }

            registeredEntities.add(entityClass);
            logInternalDebug("Entity registered for initialization: " + entityClass.getSimpleName());

        } catch (Exception e) {
            throw new DatabaseException("Entity Registration", entityClass.getSimpleName(),
                    getAdapterType(), "Failed to register entity", e);
        }
    }

    public CompletableFuture<Void> initializeAllTables() {
        return CompletableFuture.runAsync(() -> {
            synchronized (initializationLock) {
                if (tablesInitialized) {
                    logInternalDebug("Tables already initialized, skipping...");
                    return;
                }

                try {
                    logInternalDebug("=== STARTING TABLE INITIALIZATION ===");

                    if (databaseConfig.getBoolean("database.auto-migrate", true)) {
                        for (Class<?> entityClass : registeredEntities) {
                            try {
                                logInternalDebug("Initializing table for: " + entityClass.getAnnotation(Table.class).name());
                                migrationManager.createOrUpdateTable(adapter, entityClass);
                                logInternalInfo("Table initialized: " + entityClass.getAnnotation(Table.class).name());

                            } catch (Exception e) {
                                String errorMsg = "Failed to initialize table for " + entityClass.getAnnotation(Table.class).name();
                                DatabaseException dbException = new DatabaseException("Table Initialization",
                                        entityClass.getAnnotation(Table.class).name(), getAdapterType(), errorMsg, e);
                                errorHandler.handleError(dbException);
                                throw dbException;
                            }
                        }
                    }

                    tablesInitialized = true;
                    logInternalDebug("=== TABLE INITIALIZATION COMPLETED ===");

                } catch (Exception e) {
                    String errorMsg = "Critical error during table initialization";
                    DatabaseException dbException = new DatabaseException("Table Initialization",
                            "Multiple", getAdapterType(), errorMsg, e);
                    errorHandler.handleError(dbException);
                    throw dbException;
                }
            }
        }, executor);
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
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errorHandler.logWarning("Table Initialization", "System",
                        "Thread interrupted while waiting for table initialization");
                return false;
            }
        }

        if (!tablesInitialized) {
            String errorMsg = String.format("Timeout waiting for table initialization (%d seconds)", timeoutSeconds);
            DatabaseException dbException = new DatabaseException("Table Initialization",
                    "System", getAdapterType(), errorMsg);
            errorHandler.handleError(dbException);
            return false;
        }

        logInternalDebug("Tables initialized successfully");
        return true;
    }

    public boolean areTablesReady() {
        return tablesInitialized;
    }

    @SuppressWarnings("unchecked")
    public <T> Repository<T> getRepository(Class<T> entityClass) {
        try {
            if (!areTablesReady()) {
                String errorMsg = "Attempted to get repository before table initialization: " + entityClass.getSimpleName();
                logInternalError(errorMsg);

                if (!waitForTablesInitialization(5)) {
                    throw new RepositoryException("getRepository", entityClass.getSimpleName(),
                            "Tables are not initialized and timeout occurred", null);
                }
            }

            return (Repository<T>) repositories.computeIfAbsent(entityClass, clazz -> {
                try {
                    Constructor<RepositoryImpl> constructor = RepositoryImpl.class.getConstructor(
                            DatabaseAdapter.class, Class.class, ExecutorService.class, DatabaseErrorHandler.class
                    );
                    return constructor.newInstance(adapter, clazz, executor, errorHandler);

                } catch (Exception e) {
                    throw new RepositoryException("getRepository", clazz.getSimpleName(),
                            "Failed to create repository instance", e);
                }
            });

        } catch (Exception e) {
            if (e instanceof RepositoryException) {
                errorHandler.handleError((RepositoryException) e);
                throw e;
            } else {
                RepositoryException repoException = new RepositoryException("getRepository",
                        entityClass.getSimpleName(), "Unexpected error while getting repository", e);
                errorHandler.handleError(repoException);
                throw repoException;
            }
        }
    }

    public boolean performCompleteReload() {
        try {
            logInternalDebug("=== STARTING COMPLETE RELOAD ===");

            synchronized (initializationLock) {
                tablesInitialized = false;
            }

            logInternalDebug("Waiting for pending async operations...");
            try {
                Thread.sleep(2000);  
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            logInternalDebug("Reconnecting to database...");
            reconnect();

            if (!isConnected()) {
                throw new ConnectionException(getAdapterType(), "reload",
                        "Failed to establish database connection during reload", null);
            }

            logInternalDebug("Initializing tables synchronously...");
            CompletableFuture<Void> initFuture = initializeAllTables();

            try {
                initFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new DatabaseException("Table Initialization", "System", getAdapterType(),
                        "Failed during table initialization in reload", e);
            }

            logInternalDebug("Recreating all repositories...");
            recreateAllRepositories();
            return true;

        } catch (Exception e) {
            String errorMsg = "Critical error during complete database reload";
            DatabaseException dbException = new DatabaseException("Complete Reload", "System",
                    getAdapterType(), errorMsg, e);
            errorHandler.handleError(dbException);
            return false;
        }
    }

    public void recreateAllRepositories() {
        try {
            Set<Class<?>> entityClasses = new HashSet<>(repositories.keySet());
            repositories.clear();

            for (Class<?> entityClass : entityClasses) {
                try {
                    Constructor<RepositoryImpl> constructor = RepositoryImpl.class.getConstructor(
                            DatabaseAdapter.class, Class.class, ExecutorService.class, DatabaseErrorHandler.class
                    );
                    Repository<?> newRepository = constructor.newInstance(adapter, entityClass, executor, errorHandler);
                    repositories.put(entityClass, newRepository);

                } catch (Exception e) {
                    errorHandler.logWarning("Repository Recreation", entityClass.getSimpleName(),
                            "Failed to recreate repository: " + e.getMessage());
                }
            }

            logInternalDebug("Repository recreation completed");

        } catch (Exception e) {
            DatabaseException dbException = new DatabaseException("Repository Recreation", "Multiple",
                    getAdapterType(), "Failed to recreate repositories", e);
            errorHandler.handleError(dbException);
        }
    }

    public <T> CompletableFuture<T> executeAsync(DatabaseOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.execute(adapter);
            } catch (Exception e) {
                throw new DatabaseException("Async Operation", "Unknown", getAdapterType(),
                        "Failed to execute async database operation", e);
            }
        }, executor);
    }

    public <T> T executeSync(DatabaseOperation<T> operation) {
        try {
            return operation.execute(adapter);
        } catch (Exception e) {
            throw new DatabaseException("Sync Operation", "Unknown", getAdapterType(),
                    "Failed to execute sync database operation", e);
        }
    }

    public void reconnect() {
        try {
            if (adapter != null) {
                adapter.disconnect();
            }
            Thread.sleep(500);
            loadConfiguration();
            connectToDatabase();

        } catch (Exception e) {
            throw new ConnectionException(getAdapterType(), "reconnect",
                    "Failed during database reconnection", e);
        }
    }

    public boolean isConnected() {
        return adapter != null && adapter.isConnected();
    }

    public Set<Class<?>> getRegisteredEntities() {
        return new HashSet<>(registeredEntities);
    }

    public void clearRepositoryCache() {
        logInternalDebug("Clearing repository cache...");
        repositories.clear();
    }

    public CompletableFuture<ExportResult> exportDatabase(ExportOptions options) {
        return exportImportManager.exportDatabase(options);
    }

    public CompletableFuture<ImportResult> importDatabase(ImportOptions options) {
        return exportImportManager.importDatabase(options);
    }

    public CompletableFuture<ExportResult> exportEntity(Class<?> entityClass, ExportOptions options) {
        return exportImportManager.exportEntity(entityClass, options);
    }

    public CompletableFuture<ImportResult> importEntity(Class<?> entityClass, ImportOptions options) {
        return exportImportManager.importEntity(entityClass, options);
    }

    public CompletableFuture<ExportResult> createBackup() {
        return exportImportManager.createBackup();
    }

    public CompletableFuture<ImportResult> restoreBackup(String backupFileName, boolean clearExisting) {
        return exportImportManager.restoreBackup(backupFileName, clearExisting);
    }

    public List<String> listExports() {
        return exportImportManager.listExports();
    }

    public ExportMetadata getExportInfo(String fileName) {
        return exportImportManager.getExportInfo(fileName);
    }

    public boolean deleteExport(String fileName) {
        return exportImportManager.deleteExport(fileName);
    }

    public void shutdown() {
        try {
            logInternalDebug("Shutting down database connections...");

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
            logInternalInfo("Database manager shutdown completed");

        } catch (Exception e) {
            errorHandler.handleGenericError("Shutdown", "DatabaseManager", "System", e);
        }
    }

    private String getAdapterType() {
        return adapter != null ? adapter.getClass().getSimpleName() : "Unknown";
    }

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(DatabaseAdapter adapter) throws Exception;
    }
}
