package net.exylia.commons.redis;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.redis.config.RedisConfig;
import net.exylia.commons.redis.config.RedisConfigManager;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalError;

public class RedisIntegration {

    @Getter
    private static boolean autoInitialized = false;
    @Getter
    private static RedisConfigManager configManager;
    private static ExyliaPlugin currentPlugin;

    public static void init(ExyliaPlugin plugin) {
        currentPlugin = plugin;

        if (autoInitialized) {
            return;
        }

        try {
             
            if (configManager == null || !plugin.equals(configManager.getPlugin())) {
                configManager = new RedisConfigManager(plugin);
                configManager.initialize();
            } else {
                 
                configManager.reloadConfig();
            }

            if (!configManager.isRedisEnabled()) {
                logInternalInfo("Redis is disabled in redis.yml");
                return;
            }

            RedisConfig redisConfig = configManager.createRedisConfig();
            redisConfig.validate();
            RedisManager.start(plugin, redisConfig);
            autoInitialized = true;

            logInternalInfo("Redis inicializado automáticamente");

        } catch (Exception e) {
            logInternalError("Error al inicializar Redis: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        if (configManager == null) {
            logInternalError("No se puede recargar configuración: Redis no está inicializado");
            return;
        }

        try {
            configManager.reloadConfig();
            logInternalInfo("Configuración de Redis recargada.");

            logInternalInfo("Nota: Algunos cambios requieren reiniciar el servidor para aplicarse");

        } catch (Exception e) {
            logInternalError("Error al recargar configuración de Redis: " + e.getMessage());
        }
    }

    public static boolean performCompleteReload() {
        try {
            logInternalInfo("Iniciando reload completo de Redis...");

            if (autoInitialized && RedisManager.isAvailable()) {
                logInternalInfo("Cerrando conexión Redis actual...");
                RedisManager.getInstance().shutdown();
            }

            autoInitialized = false;
            configManager = null;

            Thread.sleep(500);

            if (currentPlugin != null) {
                logInternalInfo("Reinicializando Redis con nueva configuración...");
                init(currentPlugin);

                RedisStatus status = getStatus();
                if (status.isEnabledInConfig()) {
                    if (status.isFullyOperational()) {
                        logInternalInfo("Redis reload completo exitoso");
                        return true;
                    } else {
                        logInternalError("Redis habilitado pero no completamente operacional después del reload");
                        return false;
                    }
                } else {
                    logInternalInfo("Redis está deshabilitado en configuración después del reload");
                    return true;  
                }
            } else {
                logInternalError("No hay plugin disponible para reinicializar Redis");
                return false;
            }

        } catch (Exception e) {
            logInternalError("Error durante reload completo de Redis: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean forceRestart() {
        try {
            logInternalInfo("Forzando reinicio de Redis...");

            try {
                if (RedisManager.isAvailable()) {
                    RedisManager.getInstance().shutdown();
                }
            } catch (Exception e) {
                logInternalError("Error cerrando Redis (continuando): " + e.getMessage());
            }

            autoInitialized = false;
            configManager = null;

            Thread.sleep(1000);  

            if (currentPlugin != null) {
                init(currentPlugin);
                return getStatus().isFullyOperational();
            }

            return false;

        } catch (Exception e) {
            logInternalError("Error en reinicio forzado de Redis: " + e.getMessage());
            return false;
        }
    }

    public static void shutdownRedis() {
        if (autoInitialized && RedisManager.isAvailable()) {
            try {
                RedisManager.getInstance().shutdown();
                autoInitialized = false;
                configManager = null;
                currentPlugin = null;
                logInternalInfo("Redis cerrado automáticamente");
            } catch (Exception e) {
                logInternalError("Error al cerrar Redis automáticamente: " + e.getMessage());
            }
        }
    }

    public static boolean configFileExists() {
        return configManager != null && configManager.configFileExists();
    }

    public static void enableRedis() {
        if (configManager != null) {
            configManager.setRedisEnabled(true);
            configManager.saveConfig();
            logInternalInfo("Redis habilitado en redis.yml");
        }
    }

    public static void disableRedis() {
        if (configManager != null) {
            configManager.setRedisEnabled(false);
            configManager.saveConfig();
            logInternalInfo("Redis deshabilitado en redis.yml");
        }
    }

    public static RedisStatus getStatus() {
        return new RedisStatus(
                autoInitialized,
                configManager != null,
                configFileExists(),
                RedisManager.isAvailable(),
                configManager != null ? configManager.isRedisEnabled() : false
        );
    }

    public static boolean shouldBeEnabled(ExyliaPlugin plugin) {
        try {
             
            RedisConfigManager tempConfigManager = new RedisConfigManager(plugin);
            tempConfigManager.initialize();
            return tempConfigManager.isRedisEnabled();
        } catch (Exception e) {
            logInternalError("Error verificando si Redis debería estar habilitado: " + e.getMessage());
            return false;
        }
    }

    public static String getDetailedStatus() {
        RedisStatus status = getStatus();
        StringBuilder sb = new StringBuilder();
        sb.append("=== REDIS STATUS ===\n");
        sb.append("Auto-inicializado: ").append(status.isAutoInitialized()).append("\n");
        sb.append("Config Manager existe: ").append(status.isConfigManagerExists()).append("\n");
        sb.append("Archivo config existe: ").append(status.isConfigFileExists()).append("\n");
        sb.append("Redis disponible: ").append(status.isRedisAvailable()).append("\n");
        sb.append("Habilitado en config: ").append(status.isEnabledInConfig()).append("\n");
        sb.append("Completamente operacional: ").append(status.isFullyOperational()).append("\n");

        if (configManager != null) {
            sb.append("Archivo de configuración: ").append(configManager.getConfigFilePath()).append("\n");
        }

        return sb.toString();
    }

    public static class RedisStatus {
        private final boolean autoInitialized;
        private final boolean configManagerExists;
        private final boolean configFileExists;
        private final boolean redisAvailable;
        private final boolean enabledInConfig;

        public RedisStatus(boolean autoInitialized, boolean configManagerExists,
                           boolean configFileExists, boolean redisAvailable, boolean enabledInConfig) {
            this.autoInitialized = autoInitialized;
            this.configManagerExists = configManagerExists;
            this.configFileExists = configFileExists;
            this.redisAvailable = redisAvailable;
            this.enabledInConfig = enabledInConfig;
        }

        public boolean isAutoInitialized() {
            return autoInitialized;
        }

        public boolean isConfigManagerExists() {
            return configManagerExists;
        }

        public boolean isConfigFileExists() {
            return configFileExists;
        }

        public boolean isRedisAvailable() {
            return redisAvailable;
        }

        public boolean isEnabledInConfig() {
            return enabledInConfig;
        }

        public boolean isFullyOperational() {
            return autoInitialized && configManagerExists && configFileExists &&
                    redisAvailable && enabledInConfig;
        }

        @Override
        public String toString() {
            return "RedisStatus{" +
                    "autoInitialized=" + autoInitialized +
                    ", configManagerExists=" + configManagerExists +
                    ", configFileExists=" + configFileExists +
                    ", redisAvailable=" + redisAvailable +
                    ", enabledInConfig=" + enabledInConfig +
                    ", fullyOperational=" + isFullyOperational() +
                    '}';
        }
    }
}
