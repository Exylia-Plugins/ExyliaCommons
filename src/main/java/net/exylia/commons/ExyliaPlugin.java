package net.exylia.commons;

import net.exylia.commons.config.ConfigurationSystem;
import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.license.LicenseManager;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.*;
import net.exylia.commons.wizard.LocationWizardManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Clase base renovada y limpia para todos los plugins Exylia
 *
 * Versión optimizada que elimina métodos de mensajes del core para mantener
 * una arquitectura más limpia y modular. Los mensajes se manejan ahora
 * a través de MessageManager estático en cada plugin.
 */
public abstract class ExyliaPlugin extends JavaPlugin {

    // ===== STATIC FIELDS =====
    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();
    private static ExyliaPlugin instance;

    // ===== INSTANCE FIELDS =====
    private BukkitAudiences adventure;
    private LicenseManager licenseManager;
    private ConfigurationSystem configSystem;
    private final boolean requiresLicense;

    // ===== CONSTRUCTOR =====
    protected ExyliaPlugin(boolean requiresLicense) {
        this.requiresLicense = requiresLicense;
    }

    // ===== BUKKIT LIFECYCLE =====
    @Override
    public final void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        registeredPlugins.add(this);

        if (!initialized) {
            initializeExylia();
            instance = this;
            initialized = true;
        }

        // Inicializar el sistema de configuración
        initializeConfigurationSystem();

        licenseManager = new LicenseManager(this, requiresLicense);
        licenseManager.initializeAndVerify()
                .thenRun(() -> Bukkit.getScheduler().runTask(this, this::enablePlugin))
                .exceptionally(throwable -> {
                    Bukkit.getScheduler().runTask(this, () -> {
                        logError("License verification failed: " + throwable.getMessage());
                        getServer().getPluginManager().disablePlugin(this);
                    });
                    return null;
                });
    }

    @Override
    public final void onDisable() {
        registeredPlugins.remove(this);
        Bukkit.getOnlinePlayers().forEach(HumanEntity::closeInventory);
        onExyliaDisable();

        if (configSystem != null) {
            configSystem.shutdown();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (registeredPlugins.isEmpty()) {
            shutdownExylia();
            initialized = false;
        }

        logInfo("Plugin Exylia deshabilitado: " + getDescription().getName());
    }

    private void enablePlugin() {
        try {
            onExyliaEnable();
            logSuccess("Plugin Exylia habilitado correctamente: " + getDescription().getName());
        } catch (Exception e) {
            logError("Error habilitando plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // ===== SISTEMA DE CONFIGURACIÓN =====

    /**
     * Inicializa el sistema de configuración con las clases especificadas por el plugin
     */
    private void initializeConfigurationSystem() {
        try {
            configSystem = new ConfigurationSystem(this);

            // Obtener las clases de configuración del plugin
            Class<? extends ConfigBase>[] configClasses = getConfigurationClasses();

            if (configClasses != null && configClasses.length > 0) {
                configSystem.initialize(configClasses);
                setupConfigurationListeners();
                logSuccess("Sistema de configuración inicializado con " + configClasses.length + " clases");
            } else {
                logInfo("No se especificaron clases de configuración para " + getName());
            }

        } catch (Exception e) {
            logError("Error inicializando sistema de configuración: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Configura listeners para reloads de configuración
     */
    private void setupConfigurationListeners() {
        configSystem.addReloadListener(new ConfigurationSystem.ConfigReloadListener() {
            @Override
            public void onConfigReload(String fileName) {
                onConfigurationFileReload(fileName);
            }

            @Override
            public void onAllConfigsReload() {
                onAllConfigurationsReload();
            }
        });
    }

    // ===== MÉTODOS ABSTRACTOS PARA IMPLEMENTAR =====

    /**
     * Implementación principal del plugin
     */
    protected abstract void onExyliaEnable();

    /**
     * Limpieza del plugin
     */
    protected abstract void onExyliaDisable();

    /**
     * Define las clases de configuración que usa el plugin
     * @return Array de clases que extienden ConfigBase
     */
    protected abstract Class<? extends ConfigBase>[] getConfigurationClasses();

    // ===== API DE CONFIGURACIÓN =====

    /**
     * Obtiene una configuración por su clase
     * @param configClass La clase de configuración
     * @return La instancia de configuración
     */
    protected final <T extends ConfigBase> T getConfig(Class<T> configClass) {
        if (configSystem == null) {
            throw new IllegalStateException("Sistema de configuración no inicializado");
        }
        return configSystem.getConfig(configClass);
    }

    /**
     * Obtiene el sistema de configuración completo
     * @return El ConfigurationSystem
     */
    public final ConfigurationSystem getCS() {
        return configSystem;
    }

// ===== SISTEMA DE RELOAD ASÍNCRONO =====

    public final CompletableFuture<ReloadResult> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInfo("=== INICIANDO RELOAD COMPLETO ASÍNCRONO ===");

                // 1. Reload del sistema de configuración
                long configStart = System.currentTimeMillis();
                if (!configSystem.reloadAllAsync().join()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de configuraciones");
                }
                componentTimes.put("Configuración", System.currentTimeMillis() - configStart);

                // 2. Reload de base de datos
                long dbStart = System.currentTimeMillis();
                if (!reloadDatabaseAsync().join()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de base de datos");
                }
                componentTimes.put("Base de Datos", System.currentTimeMillis() - dbStart);

                // 3. Reload de Redis
                long redisStart = System.currentTimeMillis();
                if (!reloadRedisAsync().join()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de Redis");
                }
                componentTimes.put("Redis", System.currentTimeMillis() - redisStart);

                // 4. Reload personalizado del plugin
                long customStart = System.currentTimeMillis();
                Boolean customResult = Bukkit.getScheduler().callSyncMethod(this, () -> {
                    try {
                        onPluginReload();
                        return true;
                    } catch (Exception e) {
                        logError("Error en reload personalizado: " + e.getMessage());
                        return false;
                    }
                }).get();

                if (!customResult) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload personalizado");
                }
                componentTimes.put("Plugin Custom", System.currentTimeMillis() - customStart);

                long totalTime = System.currentTimeMillis() - startTime;
                logSuccess("=== RELOAD COMPLETADO EXITOSAMENTE EN " + totalTime + "ms ===");

                return new ReloadResult(true, totalTime, componentTimes, null);

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logError("Error durante reload completo: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    /**
     * Reload automático de base de datos de forma asíncrona
     */
    public final CompletableFuture<Boolean> reloadDatabaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (DatabaseManager.getInstance() != null) {
                    boolean success = DatabaseManager.getInstance().performCompleteReload();
                    if (success) {
                        // Ejecutar hook en hilo principal si es necesario
                        Bukkit.getScheduler().runTask(this, this::onDatabaseReload);
                        return true;
                    } else {
                        logError("Fallo en reload automático de base de datos");
                        return false;
                    }
                } else {
                    DatabaseManager.initialize(this);
                    if (DatabaseManager.getInstance().isConnected()) {
                        Bukkit.getScheduler().runTask(this, this::onDatabaseReload);
                        return true;
                    } else {
                        logError("Error: No se pudo inicializar la base de datos");
                        return false;
                    }
                }
            } catch (Exception e) {
                logError("Error crítico en reload de base de datos: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Reload automático de Redis de forma asíncrona
     */
    public final CompletableFuture<Boolean> reloadRedisAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logInfo("Iniciando reload de Redis...");

                boolean wasInitialized = RedisIntegration.isAutoInitialized();

                if (wasInitialized) {
                    logInfo("Redis ya estaba inicializado, realizando reload completo...");
                    boolean success = RedisIntegration.performCompleteReload();

                    if (success) {
                        logSuccess("Redis reinicializado correctamente");
                    } else {
                        logError("Fallo en reload de Redis");
                    }

                    // Ejecutar hook en hilo principal
                    Bukkit.getScheduler().runTask(this, this::onRedisReload);
                    return success;

                } else {
                    logInfo("Redis no estaba inicializado, verificando configuración...");

                    RedisIntegration.init(this);
                    RedisIntegration.RedisStatus status = RedisIntegration.getStatus();

                    if (status.isEnabledInConfig()) {
                        if (status.isFullyOperational()) {
                            logSuccess("Redis habilitado e inicializado correctamente por primera vez");
                            Bukkit.getScheduler().runTask(this, this::onRedisReload);
                            return true;
                        } else {
                            logError("Redis habilitado en configuración pero falló la inicialización");
                            Bukkit.getScheduler().runTask(this, this::onRedisReload);
                            return false;
                        }
                    } else {
                        logInfo("Redis sigue deshabilitado en redis.yml");
                        Bukkit.getScheduler().runTask(this, this::onRedisReload);
                        return true;
                    }
                }

            } catch (Exception e) {
                logError("Error crítico en reload de Redis: " + e.getMessage());
                e.printStackTrace();

                try {
                    Bukkit.getScheduler().runTask(this, this::onRedisReload);
                } catch (Exception hookError) {
                    logError("Error adicional en hook de Redis reload: " + hookError.getMessage());
                }

                return false;
            }
        });
    }

    /**
     * Reload con timeout para evitar esperas infinitas
     */
    public final CompletableFuture<ReloadResult> reloadAllAsync(long timeoutSeconds) {
        return reloadAllAsync()
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logError("Reload cancelado por timeout (" + timeoutSeconds + "s)");
                        return new ReloadResult(false, timeoutSeconds * 1000,
                                new HashMap<>(), "Timeout de " + timeoutSeconds + " segundos");
                    } else {
                        logError("Error en reload con timeout: " + throwable.getMessage());
                        return new ReloadResult(false, 0,
                                new HashMap<>(), "Error: " + throwable.getMessage());
                    }
                });
    }

    // ===== HOOKS OPCIONALES PARA LOS PLUGINS =====

    /**
     * Hook llamado después del reload de base de datos
     */
    protected void onDatabaseReload() {
        // Hook vacío por defecto
    }

    /**
     * Hook llamado después del reload de Redis
     */
    protected void onRedisReload() {
        // Hook vacío por defecto
    }

    /**
     * Hook llamado para reload personalizado del plugin
     */
    protected void onPluginReload() {
        // Hook vacío por defecto
    }

    /**
     * Hook llamado cuando se recarga un archivo de configuración específico
     * @param fileName Nombre del archivo recargado
     */
    protected void onConfigurationFileReload(String fileName) {
        // Hook vacío por defecto
    }

    /**
     * Hook llamado cuando se recargan todas las configuraciones
     */
    protected void onAllConfigurationsReload() {
        // Hook vacío por defecto
    }

    // ===== LICENSE METHODS =====
    protected final boolean shouldOperate() {
        return licenseManager != null && licenseManager.isVerified();
    }

    protected final void requireLicense(String feature) {
        if (licenseManager == null || !licenseManager.isVerified()) {
            throw new SecurityException("La función '" + feature + "' requiere una licencia válida");
        }
    }

    // ===== ADVENTURE API =====
    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Attempted to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    // ===== STATIC UTILITY METHODS =====
    @SuppressWarnings("unchecked")
    public static <T extends ExyliaPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaPlugin plugin : registeredPlugins) {
            if (pluginClass.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static ExyliaPlugin getInstance() {
        return instance;
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    // ===== PRIVATE INITIALIZATION METHODS =====
    private void initializeExylia() {
        try {
            AdapterFactory.initialize(this);
            ActionBarUtils.init(this);
            BossbarUtils.init(this);
            TitleUtils.init(this);
        } catch (Exception e) {
            logInfo("Error inicializando un sistema: " + e.getMessage());
        }
        checkOptionalDependencies();
        logInfo("Núcleo Exylia inicializado correctamente");
    }

    private void checkOptionalDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logInfo("PlaceholderAPI detectado.");
        }
        try {
            Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException ignored) {
        }
        checkDatabaseDrivers();
    }

    private void checkDatabaseDrivers() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            Class.forName("com.mongodb.client.MongoClient");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void shutdownExylia() {
        logInfo("Limpiando recursos globales de Exylia");
        try {
            if (DatabaseManager.getInstance() != null) {
                DatabaseManager.getInstance().shutdown();
            }
        } catch (Exception e) {
            logInfo("Error cerrando sistema de base de datos: " + e.getMessage());
        }
        try {
            if (LocationWizardManager.getInstance() != null) {
                LocationWizardManager.getInstance().cleanup();
            }
        } catch (Exception e) {
            logInfo("Error limpiando wizards: " + e.getMessage());
        }
        RedisIntegration.shutdownRedis();
        ColorUtils.shutdown();
        OldColorUtils.shutdown();
        AdapterFactory.close();
        PlaceholderRegistry.clear();
    }
}