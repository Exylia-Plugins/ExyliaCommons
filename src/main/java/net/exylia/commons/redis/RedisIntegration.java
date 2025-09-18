package net.exylia.commons.redis;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.redis.config.RedisConfig;
import net.exylia.commons.redis.config.RedisConfigManager;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalError;

/**
 * Clase de integración para configurar Redis automáticamente en ExyliaPlugin
 * Usa un archivo redis.yml dedicado para la configuración
 */
public class RedisIntegration {

    @Getter
    private static boolean autoInitialized = false;
    @Getter
    private static RedisConfigManager configManager;
    private static ExyliaPlugin currentPlugin;

    /**
     * Inicializa Redis automáticamente creando y usando el archivo redis.yml
     */
    public static void init(ExyliaPlugin plugin) {
        currentPlugin = plugin;

        // Si ya está inicializado, no hacer nada
        if (autoInitialized) {
            return;
        }

        try {
            // Si no hay configManager o si es diferente plugin, crear/recrear
            if (configManager == null || !plugin.equals(configManager.getPlugin())) {
                configManager = new RedisConfigManager(plugin);
                configManager.initialize();
            } else {
                // Si ya existe configManager para este plugin, recargar config
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

    /**
     * Recarga la configuración de Redis desde el archivo
     */
    public static void reloadConfig() {
        if (configManager == null) {
            logInternalError("No se puede recargar configuración: Redis no está inicializado");
            return;
        }

        try {
            configManager.reloadConfig();
            logInternalInfo("Configuración de Redis recargada.");

            // Nota: Para aplicar cambios de configuración completamente,
            // sería necesario reinicializar Redis, pero esto puede ser disruptivo
            logInternalInfo("Nota: Algunos cambios requieren reiniciar el servidor para aplicarse");

        } catch (Exception e) {
            logInternalError("Error al recargar configuración de Redis: " + e.getMessage());
        }
    }

    /**
     * NUEVO: Reload completo de Redis - cierra conexión actual y reinicializa con nueva configuración
     */
    public static boolean performCompleteReload() {
        try {
            logInternalInfo("Iniciando reload completo de Redis...");

            // 1. Cerrar conexión actual si existe
            if (autoInitialized && RedisManager.isAvailable()) {
                logInternalInfo("Cerrando conexión Redis actual...");
                RedisManager.getInstance().shutdown();
            }

            // 2. Resetear estado
            autoInitialized = false;
            configManager = null;

            // 3. Pequeña pausa para asegurar cierre completo
            Thread.sleep(500);

            // 4. Reinicializar desde cero
            if (currentPlugin != null) {
                logInternalInfo("Reinicializando Redis con nueva configuración...");
                init(currentPlugin);

                // 5. Verificar resultado
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
                    return true; // No es error si está intencionalmente deshabilitado
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

    /**
     * Reinicia Redis forzadamente - útil para debugging
     */
    public static boolean forceRestart() {
        try {
            logInternalInfo("Forzando reinicio de Redis...");

            // Cerrar sin importar el estado
            try {
                if (RedisManager.isAvailable()) {
                    RedisManager.getInstance().shutdown();
                }
            } catch (Exception e) {
                logInternalError("Error cerrando Redis (continuando): " + e.getMessage());
            }

            // Reset completo
            autoInitialized = false;
            configManager = null;

            Thread.sleep(1000); // Pausa más larga para forzar

            // Reinicializar
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

    /**
     * Cierra Redis si fue inicializado automáticamente
     */
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

    /**
     * Verifica si el archivo redis.yml existe
     */
    public static boolean configFileExists() {
        return configManager != null && configManager.configFileExists();
    }

    /**
     * Habilita Redis en la configuración y guarda el archivo
     */
    public static void enableRedis() {
        if (configManager != null) {
            configManager.setRedisEnabled(true);
            configManager.saveConfig();
            logInternalInfo("Redis habilitado en redis.yml");
        }
    }

    /**
     * Deshabilita Redis en la configuración y guarda el archivo
     */
    public static void disableRedis() {
        if (configManager != null) {
            configManager.setRedisEnabled(false);
            configManager.saveConfig();
            logInternalInfo("Redis deshabilitado en redis.yml");
        }
    }

    /**
     * Obtiene información del estado actual de Redis
     */
    public static RedisStatus getStatus() {
        return new RedisStatus(
                autoInitialized,
                configManager != null,
                configFileExists(),
                RedisManager.isAvailable(),
                configManager != null ? configManager.isRedisEnabled() : false
        );
    }

    /**
     * Verifica si Redis debería estar habilitado según la configuración actual
     * Útil para detectar cambios sin inicializar Redis
     */
    public static boolean shouldBeEnabled(ExyliaPlugin plugin) {
        try {
            // Crear un configManager temporal solo para leer
            RedisConfigManager tempConfigManager = new RedisConfigManager(plugin);
            tempConfigManager.initialize();
            return tempConfigManager.isRedisEnabled();
        } catch (Exception e) {
            logInternalError("Error verificando si Redis debería estar habilitado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene información detallada del estado para debugging
     */
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

    // ==================== CLASE DE ESTADO ====================

    /**
     * Información del estado de Redis
     */
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