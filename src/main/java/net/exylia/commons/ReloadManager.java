package net.exylia.commons;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.ReloadResult;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.redis.RedisIntegration;
import net.exylia.commons.utils.DateFormatter;
import net.exylia.commons.utils.TimeFormatter;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Gestor centralizado para todos los tipos de reload del sistema Exylia
 */
public class ReloadManager {

    private final ExyliaPlugin plugin;

    public ReloadManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== RELOAD COMPLETO =====

    /**
     * Realiza un reload completo de todos los sistemas
     */
    public CompletableFuture<ReloadResult> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalInfo("=== INICIANDO RELOAD COMPLETO ASÍNCRONO ===");

                // 1. Reload del sistema de configuración
                ReloadResult configResult = reloadConfigurationAsync().join();
                componentTimes.put("Configuración", configResult.getDurationMs());
                if (!configResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de configuraciones: " + configResult.getErrorMessage());
                }

                // 2. Reload de base de datos
                ReloadResult dbResult = reloadDatabaseAsync().join();
                componentTimes.put("Base de Datos", dbResult.getDurationMs());
                if (!dbResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de base de datos: " + dbResult.getErrorMessage());
                }

                // 3. Reload de Redis
                ReloadResult redisResult = reloadRedisAsync().join();
                componentTimes.put("Redis", redisResult.getDurationMs());
                if (!redisResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de Redis: " + redisResult.getErrorMessage());
                }

                // 4. Reload personalizado del plugin
                ReloadResult pluginResult = reloadPluginAsync().join();
                componentTimes.put("Plugin Custom", pluginResult.getDurationMs());
                if (!pluginResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload personalizado: " + pluginResult.getErrorMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logInternalSuccess("=== RELOAD COMPLETED IN " + totalTime + "ms ===");

                return new ReloadResult(true, totalTime, componentTimes, null);
            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error durante reload completo: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    /**
     * Realiza un reload completo con timeout
     */
    public CompletableFuture<ReloadResult> reloadAllAsync(long timeoutSeconds) {
        return reloadAllAsync()
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logInternalError("Reload cancelado por timeout (" + timeoutSeconds + "s)");
                        return new ReloadResult(false, timeoutSeconds * 1000,
                                new HashMap<>(), "Timeout de " + timeoutSeconds + " segundos");
                    } else {
                        logInternalError("Error en reload con timeout: " + throwable.getMessage());
                        return new ReloadResult(false, 0,
                                new HashMap<>(), "Error: " + throwable.getMessage());
                    }
                });
    }

    // ===== RELOAD ESPECÍFICOS =====

    /**
     * Reload solo del sistema de configuración
     */
    public CompletableFuture<ReloadResult> reloadConfigurationAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalInfo("Iniciando reload de configuración...");

                long configStart = System.currentTimeMillis();
                boolean success = ConfigManager.reloadAllAsync().join();
                long configTime = System.currentTimeMillis() - configStart;
                componentTimes.put("ConfigManager", configTime);

                if (!success) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Fallo en reload de ConfigManager");
                }

                // Reload de TimeFormatter
                long timeFormatterStart = System.currentTimeMillis();
                TimeFormatter.reload();
                DateFormatter.reload();
                componentTimes.put("TimeFormatter", System.currentTimeMillis() - timeFormatterStart);

                // Ejecutar hooks en hilo principal
                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    plugin.callAllConfigurationsReloadHook();
                }, task -> Bukkit.getScheduler().runTask(plugin, task));

                try {
                    hookFuture.get();
                } catch (Exception e) {
                    logInternalError("Error ejecutando hooks de configuración: " + e.getMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logInternalSuccess("Configuration reload completed in " + totalTime + "ms");

                return new ReloadResult(true, totalTime, componentTimes, null);

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error en reload de configuración: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    /**
     * Reload solo de la base de datos
     */
    public CompletableFuture<ReloadResult> reloadDatabaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug(debug(), "Starting database reload...");

                long dbStart = System.currentTimeMillis();
                boolean success = false;

                if (DatabaseManager.getInstance() != null) {
                    success = DatabaseManager.getInstance().performCompleteReload();
                    componentTimes.put("DatabaseManager.reload", System.currentTimeMillis() - dbStart);

                    if (!success) {
                        return new ReloadResult(false, System.currentTimeMillis() - startTime,
                                componentTimes, "Fallo en reload automático de base de datos");
                    }
                } else {
                    DatabaseManager.initialize(plugin);
                    componentTimes.put("DatabaseManager.initialize", System.currentTimeMillis() - dbStart);

                    if (!DatabaseManager.getInstance().isConnected()) {
                        return new ReloadResult(false, System.currentTimeMillis() - startTime,
                                componentTimes, "No se pudo inicializar la base de datos");
                    }
                    success = true;
                }

                // Ejecutar hook en hilo principal
                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    plugin.callDatabaseReloadHook();
                }, task -> Bukkit.getScheduler().runTask(plugin, task));

                try {
                    hookFuture.get();
                } catch (Exception e) {
                    logInternalError("Error ejecutando hook de base de datos: " + e.getMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logInternalSuccess("Database reload completed in " + totalTime + "ms");

                return new ReloadResult(true, totalTime, componentTimes, null);

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error crítico en reload de base de datos: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    /**
     * Reload solo de Redis
     */
    public CompletableFuture<ReloadResult> reloadRedisAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug(debug(), "Iniciando reload de Redis...");

                // Verificar si Redis está disponible en el classpath
                if (!isRedisAvailable()) {
                    logInternalDebug(debug(), "Redis no está disponible en el classpath, omitiendo reload de Redis");
                    return new ReloadResult(true, System.currentTimeMillis() - startTime,
                            componentTimes, null);
                }

                long redisStart = System.currentTimeMillis();
                boolean wasInitialized = RedisIntegration.isAutoInitialized();
                boolean success = false;
                String operation = "";

                if (wasInitialized) {
                    logInternalInfo("Redis ya estaba inicializado, realizando reload completo...");
                    operation = "RedisIntegration.reload";
                    success = RedisIntegration.performCompleteReload();
                } else {
                    logInternalInfo("Redis no estaba inicializado, verificando configuración...");
                    operation = "RedisIntegration.initialize";
                    RedisIntegration.init(plugin);
                    RedisIntegration.RedisStatus status = RedisIntegration.getStatus();

                    if (status.isEnabledInConfig()) {
                        success = status.isFullyOperational();
                        if (!success) {
                            componentTimes.put(operation, System.currentTimeMillis() - redisStart);
                            return new ReloadResult(false, System.currentTimeMillis() - startTime,
                                    componentTimes, "Redis habilitado en configuración pero falló la inicialización");
                        }
                    } else {
                        logInternalInfo("Redis sigue deshabilitado en redis.yml");
                        success = true; // Redis deshabilitado no es un error
                    }
                }

                componentTimes.put(operation, System.currentTimeMillis() - redisStart);

                // Ejecutar hook en hilo principal
                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    plugin.callRedisReloadHook();
                }, task -> Bukkit.getScheduler().runTask(plugin, task));

                try {
                    hookFuture.get();
                } catch (Exception e) {
                    logInternalError("Error ejecutando hook de Redis: " + e.getMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;
                if (success) {
                    logInternalSuccess("Reload de Redis completado en " + totalTime + "ms");
                } else {
                    logInternalError("Fallo en reload de Redis");
                }

                return new ReloadResult(success, totalTime, componentTimes,
                        success ? null : "Fallo en reload de Redis");

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error crítico en reload de Redis: " + e.getMessage());
                e.printStackTrace();

                CompletableFuture<Void> errorHookFuture = CompletableFuture.runAsync(() -> {
                    plugin.callRedisReloadHook();
                }, task -> Bukkit.getScheduler().runTask(plugin, task));

                try {
                    errorHookFuture.get();
                } catch (Exception hookError) {
                    logInternalError("Error adicional en hook de Redis reload: " + hookError.getMessage());
                }

                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    /**
     * Reload personalizado del plugin
     */
    public CompletableFuture<ReloadResult> reloadPluginAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug(debug(), "Iniciando reload personalizado del plugin...");

                long pluginStart = System.currentTimeMillis();
                CompletableFuture<Boolean> pluginFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        plugin.callPluginReloadHook();
                        return true;
                    } catch (Exception e) {
                        logInternalError("Error en reload personalizado: " + e.getMessage());
                        return false;
                    }
                }, task -> Bukkit.getScheduler().runTask(plugin, task));

                Boolean success = pluginFuture.get();

                componentTimes.put("Plugin.onPluginReload", System.currentTimeMillis() - pluginStart);

                long totalTime = System.currentTimeMillis() - startTime;
                if (success) {
                    logInternalSuccess("Plugin reload completed in " + totalTime + "ms");
                } else {
                    logInternalError("Fallo en reload personalizado");
                }

                return new ReloadResult(success, totalTime, componentTimes,
                        success ? null : "Error en reload personalizado");

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error crítico en reload personalizado: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Verifica si Redis está disponible en el classpath
     */
    private boolean isRedisAvailable() {
        try {
            Class.forName("redis.clients.jedis.exceptions.JedisException");
            Class.forName("redis.clients.jedis.Jedis");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}