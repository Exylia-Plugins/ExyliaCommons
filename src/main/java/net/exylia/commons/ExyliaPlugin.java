package net.exylia.commons;

import lombok.Getter;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigurationSystem;
import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.license.LicenseManager;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    @Getter
    private static ExyliaPlugin instance;

    // ===== INSTANCE FIELDS =====
    private BukkitAudiences adventure;
    private LicenseManager licenseManager;
    private ConfigurationSystem configSystem;
    private ReloadManager reloadManager;
    private final boolean requiresLicense;

    // ===== CONSTRUCTOR =====
    protected ExyliaPlugin(boolean requiresLicense) {
        this.requiresLicense = requiresLicense;
    }

    // ===== BUKKIT LIFECYCLE =====
    @Override
    public final void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        this.reloadManager = new ReloadManager(this);
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
                        logInternalError("License verification failed: " + throwable.getMessage());
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

        logInternalInfo("Plugin Exylia deshabilitado: " + getDescription().getName());
    }

    private void enablePlugin() {
        try {
            onExyliaEnable();
        } catch (Exception e) {
            logInternalError("Error habilitando plugin: " + e.getMessage());
            e.printStackTrace();
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
            Class<? extends ConfigBase>[] pluginConfigClasses = getConfigurationClasses();

            // Crear lista combinada con configuraciones base y del plugin
            List<Class<? extends ConfigBase>> allConfigClasses = new ArrayList<>();

            // Agregar configuraciones base de ExyliaCommons
            allConfigClasses.add(MainConfigBase.class);
            allConfigClasses.add(MessagesBase.class);

            // Agregar configuraciones específicas del plugin
            if (pluginConfigClasses != null && pluginConfigClasses.length > 0) {
                // Filtrar para evitar duplicados y mantener las extensiones
                for (Class<? extends ConfigBase> pluginClass : pluginConfigClasses) {

                    // Verificar si extiende una configuración base
                    if (MainConfigBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MainConfigBase.class)) {
                        // Reemplazar MainConfigBase con la extensión
                        allConfigClasses.removeIf(cls -> cls.equals(MainConfigBase.class));
                    }

                    if (MessagesBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MessagesBase.class)) {
                        // Reemplazar MessagesBase con la extensión
                        allConfigClasses.removeIf(cls -> cls.equals(MessagesBase.class));
                    }

                    // Agregar la clase del plugin
                    allConfigClasses.add(pluginClass);
                }
            }

            // Convertir a array
            Class<? extends ConfigBase>[] finalConfigClasses = allConfigClasses.toArray(new Class[0]);

            if (finalConfigClasses.length > 0) {
                configSystem.initialize(finalConfigClasses);
                setupConfigurationListeners();
                logInternalSuccess("Sistema de configuración inicializado con " + finalConfigClasses.length + " clases");
            } else {
                logInternalInfo("No se especificaron clases de configuración para " + getName());
            }

            // FIX: Pasar el sistema ya inicializado a ConfigManager en lugar de crear uno nuevo
            ConfigManager.init(configSystem, finalConfigClasses);
            TimeFormatter.init();
        } catch (Exception e) {
            logInternalError("Error inicializando sistema de configuración: " + e.getMessage());
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

    // ===== API DE RELOAD =====

    /**
     * Obtiene el gestor de reloads para este plugin
     * @return ReloadManager asociado a este plugin
     */
    public final ReloadManager getReloadManager() {
        return reloadManager;
    }

    /**
     * Realiza un reload completo de todos los sistemas
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadAllAsync() {
        return reloadManager.reloadAllAsync();
    }

    /**
     * Realiza un reload completo con timeout
     * @param timeoutSeconds Timeout en segundos
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadAllAsync(long timeoutSeconds) {
        return reloadManager.reloadAllAsync(timeoutSeconds);
    }

    /**
     * Realiza un reload solo del sistema de configuración
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadConfigurationAsync() {
        return reloadManager.reloadConfigurationAsync();
    }

    /**
     * Realiza un reload solo de la base de datos
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadDatabaseAsync() {
        return reloadManager.reloadDatabaseAsync();
    }

    /**
     * Realiza un reload solo de Redis
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadRedisAsync() {
        return reloadManager.reloadRedisAsync();
    }

    /**
     * Realiza un reload personalizado del plugin
     * @return CompletableFuture con el resultado del reload
     */
    public final CompletableFuture<ReloadResult> reloadPluginAsync() {
        return reloadManager.reloadPluginAsync();
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

    // ===== MÉTODOS PACKAGE-PRIVATE PARA RELOADMANAGER =====

    final void callDatabaseReloadHook() {
        onDatabaseReload();
    }

    final void callRedisReloadHook() {
        onRedisReload();
    }

    final void callPluginReloadHook() {
        onPluginReload();
    }

    final void callConfigurationFileReloadHook(String fileName) {
        onConfigurationFileReload(fileName);
    }

    final void callAllConfigurationsReloadHook() {
        onAllConfigurationsReload();
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

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    // ===== PRIVATE INITIALIZATION METHODS =====
    private void initializeExylia() {
        try {
            PlaceholderSystemManager.initialize(this);
            AdapterFactory.initialize(this);
            ActionBarUtils.init(this);
            BossbarUtils.init(this);
            TitleUtils.init(this);
        } catch (Exception e) {
            logInternalInfo("Error inicializando un sistema: " + e.getMessage());
        }
        checkOptionalDependencies();
    }

    private void checkOptionalDependencies() {
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
        logInternalInfo("Limpiando recursos globales de Exylia");
        try {
            if (DatabaseManager.getInstance() != null) {
                DatabaseManager.getInstance().shutdown();
            }
        } catch (Exception e) {
            logInternalInfo("Error cerrando sistema de base de datos: " + e.getMessage());
        }
        RedisIntegration.shutdownRedis();
        ColorUtils.shutdown();
        OldColorUtils.shutdown();
        AdapterFactory.close();
    }
}