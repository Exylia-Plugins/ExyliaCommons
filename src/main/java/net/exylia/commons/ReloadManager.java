package net.exylia.commons;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.ReloadResult;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.configSimple.Configs;
import net.exylia.commons.database.DatabaseManager;
import net.exylia.commons.simpleredis.SimpleRedis;
import net.exylia.commons.utils.DateFormatter;
import net.exylia.commons.utils.TimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.exylia.commons.utils.DebugUtils.*;

public class ReloadManager {

    private final ExyliaPlugin plugin;

    public ReloadManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<ReloadResult> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();
            long lastStepTime = startTime;

            try {
                logInternalInfo("=== INICIANDO RELOAD COMPLETO ASÍNCRONO ===");

                long stepStart = System.currentTimeMillis();
                ReloadResult configResult = reloadConfigurationAsync().join();
                long stepEnd = System.currentTimeMillis();
                componentTimes.put("Configuración", configResult.getDurationMs());
                componentTimes.put("Configuración + Overhead", stepEnd - stepStart);
                lastStepTime = stepEnd;

                Configs.reloadAll();

                if (!configResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de configuraciones: " + configResult.getErrorMessage());
                }

                stepStart = System.currentTimeMillis();
                ReloadResult dbResult = reloadDatabaseAsync().join();
                stepEnd = System.currentTimeMillis();
                componentTimes.put("Base de Datos", dbResult.getDurationMs());
                componentTimes.put("Base de Datos + Overhead", stepEnd - stepStart);
                lastStepTime = stepEnd;

                if (!dbResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de base de datos: " + dbResult.getErrorMessage());
                }

                stepStart = System.currentTimeMillis();
                ReloadResult redisResult = reloadRedisAsync().join();
                stepEnd = System.currentTimeMillis();
                componentTimes.put("Redis", redisResult.getDurationMs());
                componentTimes.put("Redis + Overhead", stepEnd - stepStart);
                lastStepTime = stepEnd;

                if (!redisResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload de Redis: " + redisResult.getErrorMessage());
                }

                stepStart = System.currentTimeMillis();
                ReloadResult pluginResult = reloadPluginAsync().join();
                stepEnd = System.currentTimeMillis();
                componentTimes.put("Plugin Custom", pluginResult.getDurationMs());
                componentTimes.put("Plugin Custom + Overhead", stepEnd - stepStart);
                lastStepTime = stepEnd;

                if (!pluginResult.isSuccess()) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Error en reload personalizado: " + pluginResult.getErrorMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;

                long totalComponentTime = componentTimes.get("Configuración") +
                                        componentTimes.get("Base de Datos") +
                                        componentTimes.get("Redis") +
                                        componentTimes.get("Plugin Custom");
                long totalOverhead = totalTime - totalComponentTime;
                componentTimes.put("Total Overhead", totalOverhead);

                logInternalSuccess("=== RELOAD COMPLETED IN " + totalTime + "ms (Components: " + totalComponentTime + "ms, Overhead: " + totalOverhead + "ms) ===");

                return new ReloadResult(true, totalTime, componentTimes, null);
            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error durante reload completo: " + e.getMessage());
                e.printStackTrace();
                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

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

                Configs.reloadAll();

                if (!success) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Fallo en reload de ConfigManager");
                }

                long timeFormatterStart = System.currentTimeMillis();
                TimeFormatter.reload();
                DateFormatter.reload();
                componentTimes.put("TimeFormatter", System.currentTimeMillis() - timeFormatterStart);

                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    Schedulers.sync(() -> plugin.callAllConfigurationsReloadHook());
                });

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

    public CompletableFuture<ReloadResult> reloadDatabaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug("Starting database reload...");

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

                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    Schedulers.sync(() -> plugin.callDatabaseReloadHook());
                });

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

    public CompletableFuture<ReloadResult> reloadRedisAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug("Iniciando reload de SimpleRedis...");

                if (!isRedisAvailable()) {
                    logInternalDebug("Redis no está disponible en el classpath, omitiendo reload de Redis");
                    return new ReloadResult(true, System.currentTimeMillis() - startTime,
                            componentTimes, null);
                }

                long redisStart = System.currentTimeMillis();
                boolean success = SimpleRedis.reload(plugin);
                componentTimes.put("SimpleRedis.reload", System.currentTimeMillis() - redisStart);

                if (!success) {
                    return new ReloadResult(false, System.currentTimeMillis() - startTime,
                            componentTimes, "Failed to reload SimpleRedis");
                }

                CompletableFuture<Void> hookFuture = CompletableFuture.runAsync(() -> {
                    Schedulers.sync(() -> plugin.callRedisReloadHook());
                });

                try {
                    hookFuture.get();
                } catch (Exception e) {
                    logInternalError("Error ejecutando hook de Redis: " + e.getMessage());
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logInternalSuccess("SimpleRedis reload completed in " + totalTime + "ms");

                return new ReloadResult(true, totalTime, componentTimes, null);

            } catch (Exception e) {
                long totalTime = System.currentTimeMillis() - startTime;
                logInternalError("Error crítico en reload de SimpleRedis: " + e.getMessage());
                e.printStackTrace();

                CompletableFuture<Void> errorHookFuture = CompletableFuture.runAsync(() -> {
                    Schedulers.sync(() -> plugin.callRedisReloadHook());
                });

                try {
                    errorHookFuture.get();
                } catch (Exception hookError) {
                    logInternalError("Error adicional en hook de Redis reload: " + hookError.getMessage());
                }

                return new ReloadResult(false, totalTime, componentTimes, e.getMessage());
            }
        });
    }

    public CompletableFuture<ReloadResult> reloadPluginAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Long> componentTimes = new HashMap<>();

            try {
                logInternalDebug("Iniciando reload personalizado del plugin...");

                long pluginStart = System.currentTimeMillis();
                CompletableFuture<Boolean> pluginFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        Schedulers.sync(() -> plugin.callPluginReloadHook());
                        return true;
                    } catch (Exception e) {
                        logInternalError("Error en reload personalizado: " + e.getMessage());
                        return false;
                    }
                });

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
