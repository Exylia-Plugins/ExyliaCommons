package net.exylia.commons.redis.config;

import net.exylia.commons.ExyliaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

/**
 * Gestor de configuración para Redis simplificado
 * Siguiendo el patrón del DatabaseManager
 */
public class RedisConfigManager {

    private static final String CONFIG_FILE_NAME = "redis.yml";
    private final ExyliaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public RedisConfigManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
    }

    /**
     * Inicializa la configuración
     */
    public void initialize() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        logInternalInfo("Configuración de Redis cargada desde redis.yml");
    }

    /**
     * Crea el archivo de configuración por defecto usando código directo
     */
    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

            // Configuración habilitado/deshabilitado
            defaultConfig.set("enabled", false);

            // Configuración de conexión
            defaultConfig.set("connection.host", "localhost");
            defaultConfig.set("connection.port", 6379);
            defaultConfig.set("connection.password", "");
            defaultConfig.set("connection.database", 0);
            defaultConfig.set("connection.timeout", 2000);
            defaultConfig.set("connection.ssl", false);

            // Configuración del pool de conexiones
            defaultConfig.set("pool.max-total", 20);
            defaultConfig.set("pool.max-idle", 10);
            defaultConfig.set("pool.min-idle", 2);
            defaultConfig.set("pool.max-wait-millis", 3000);
            defaultConfig.set("pool.test-on-borrow", true);
            defaultConfig.set("pool.test-on-return", false);
            defaultConfig.set("pool.test-while-idle", true);
            defaultConfig.set("pool.time-between-eviction-runs-millis", 30000);

            // Configuración de caché
            defaultConfig.set("cache.default-ttl", 3600);
            defaultConfig.set("cache.key-prefix", "exylia:");

            // Configuración de Pub/Sub
            defaultConfig.set("pubsub.enabled", true);
            defaultConfig.set("pubsub.prefix", "exylia:pubsub:");

            // Configuración de rendimiento
            defaultConfig.set("performance.use-local-cache", true);
            defaultConfig.set("performance.local-cache-max-size", 1000);
            defaultConfig.set("performance.local-cache-ttl", 300000);

            // Configuración de logging
            defaultConfig.set("logging.level", "INFO");
            defaultConfig.set("logging.log-pool-stats-interval", 300);
            defaultConfig.set("logging.log-slow-operations", true);
            defaultConfig.set("logging.slow-operation-threshold", 100);

            // Configuración de mantenimiento
            defaultConfig.set("maintenance.cleanup-interval", 60);
            defaultConfig.set("maintenance.health-check-interval", 30);
            defaultConfig.set("maintenance.auto-retry-attempts", 3);
            defaultConfig.set("maintenance.auto-retry-delay", 5000);

            defaultConfig.save(configFile);
            logInternalInfo("Archivo redis.yml creado con configuración por defecto");

        } catch (IOException e) {
            logInternalError("Error creando archivo redis.yml: " + e.getMessage());
            throw new RuntimeException("No se pudo crear redis.yml", e);
        }
    }

    /**
     * Recarga la configuración desde el archivo
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        logInternalInfo("Configuración de Redis recargada");
    }

    /**
     * Guarda la configuración actual al archivo
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            logInternalInfo("Configuración de Redis guardada");
        } catch (IOException e) {
            logInternalError("Error guardando configuración de Redis: " + e.getMessage());
        }
    }

    /**
     * Crea un RedisConfig desde la configuración cargada
     */
    public RedisConfig createRedisConfig() {
        if (config == null) {
            throw new IllegalStateException("Configuración no cargada. Llama a initialize() primero.");
        }

        RedisConfig.Builder builder = new RedisConfig.Builder();

        // Configuración de conexión
        builder.host(config.getString("connection.host", "localhost"))
                .port(config.getInt("connection.port", 6379))
                .database(config.getInt("connection.database", 0))
                .timeout(config.getInt("connection.timeout", 2000))
                .ssl(config.getBoolean("connection.ssl", false));

        String password = config.getString("connection.password", "");
        if (!password.trim().isEmpty()) {
            builder.password(password);
        }

        // Configuración del pool
        builder.maxTotal(config.getInt("pool.max-total", 20))
                .maxIdle(config.getInt("pool.max-idle", 10))
                .minIdle(config.getInt("pool.min-idle", 2))
                .maxWaitMillis(config.getLong("pool.max-wait-millis", 3000))
                .testOnBorrow(config.getBoolean("pool.test-on-borrow", true));

        // Configuración de caché
        builder.defaultTTL(config.getInt("cache.default-ttl", 3600))
                .keyPrefix(config.getString("cache.key-prefix", "exylia:"));

        // Configuración de Pub/Sub
        builder.enablePubSub(config.getBoolean("pubsub.enabled", true))
                .pubSubPrefix(config.getString("pubsub.prefix", "exylia:pubsub:"));

        return builder.build();
    }

    /**
     * Verifica si Redis está habilitado en la configuración
     */
    public boolean isRedisEnabled() {
        return config != null && config.getBoolean("enabled", true);
    }

    /**
     * Habilita o deshabilita Redis en la configuración
     */
    public void setRedisEnabled(boolean enabled) {
        if (config != null) {
            config.set("enabled", enabled);
        }
    }

    /**
     * Obtiene un valor de configuración
     */
    public <T> T getConfigValue(String path, T defaultValue) {
        if (config == null) {
            return defaultValue;
        }

        Object value = config.get(path, defaultValue);
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            logInternalError("Error obteniendo valor de configuración '" + path + "': tipo incorrecto");
            return defaultValue;
        }
    }

    /**
     * Establece un valor de configuración
     */
    public void setConfigValue(String path, Object value) {
        if (config != null) {
            config.set(path, value);
        }
    }

    /**
     * Verifica si existe el archivo de configuración
     */
    public boolean configFileExists() {
        return configFile.exists();
    }

    /**
     * Obtiene la ruta del archivo de configuración
     */
    public String getConfigFilePath() {
        return configFile.getAbsolutePath();
    }

    // ==================== GETTERS ====================

    public FileConfiguration getConfig() {
        return config;
    }

    public File getConfigFile() {
        return configFile;
    }

    public ExyliaPlugin getPlugin() {
        return plugin;
    }
}