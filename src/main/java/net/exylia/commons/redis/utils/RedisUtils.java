package net.exylia.commons.redis.utils;

import net.exylia.commons.redis.RedisManager;
import net.exylia.commons.redis.cache.RedisCache;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utilidades y helpers para operaciones comunes con Redis
 */
public class RedisUtils {

    private static final String PLAYER_PREFIX = "player:";
    private static final String SERVER_PREFIX = "server:";
    private static final String GLOBAL_PREFIX = "global:";

    // ==================== OPERACIONES DE JUGADOR ====================

    /**
     * Guarda datos de un jugador
     */
    public static <T> void savePlayerData(String playerName, String key, T data) {
        savePlayerData(playerName, key, data, 3600); // 1 hora por defecto
    }

    /**
     * Guarda datos de un jugador con TTL personalizado
     */
    public static <T> void savePlayerData(String playerName, String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
    }

    /**
     * Carga datos de un jugador
     */
    public static <T> T loadPlayerData(String playerName, String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    /**
     * Carga datos de un jugador o usa valor por defecto
     */
    public static <T> T loadPlayerDataOrDefault(String playerName, String key, Class<T> type, T defaultValue) {
        T data = loadPlayerData(playerName, key, type);
        return data != null ? data : defaultValue;
    }

    /**
     * Elimina datos de un jugador
     */
    public static void deletePlayerData(String playerName, String key) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        RedisManager.getInstance().delete(redisKey);
    }

    /**
     * Verifica si existen datos de un jugador
     */
    public static boolean hasPlayerData(String playerName, String key) {
        if (!RedisManager.isAvailable()) return false;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        return RedisManager.getInstance().exists(redisKey);
    }

    // ==================== OPERACIONES DE SERVIDOR ====================

    /**
     * Guarda datos del servidor
     */
    public static <T> void saveServerData(String serverName, String key, T data) {
        saveServerData(serverName, key, data, -1); // Sin expiración por defecto
    }

    /**
     * Guarda datos del servidor con TTL
     */
    public static <T> void saveServerData(String serverName, String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = SERVER_PREFIX + serverName + ":" + key;
        if (ttlSeconds > 0) {
            RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
        } else {
            RedisManager.getInstance().setObject(redisKey, data);
        }
    }

    /**
     * Carga datos del servidor
     */
    public static <T> T loadServerData(String serverName, String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = SERVER_PREFIX + serverName + ":" + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    // ==================== OPERACIONES GLOBALES ====================

    /**
     * Guarda datos globales (compartidos entre todos los servidores)
     */
    public static <T> void saveGlobalData(String key, T data) {
        saveGlobalData(key, data, -1);
    }

    /**
     * Guarda datos globales con TTL
     */
    public static <T> void saveGlobalData(String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = GLOBAL_PREFIX + key;
        if (ttlSeconds > 0) {
            RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
        } else {
            RedisManager.getInstance().setObject(redisKey, data);
        }
    }

    /**
     * Carga datos globales
     */
    public static <T> T loadGlobalData(String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = GLOBAL_PREFIX + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    // ==================== OPERACIONES DE CACHÉ AVANZADAS ====================

    /**
     * Obtiene una caché específica para jugadores
     */
    public static <T> RedisCache<T> getPlayerCache(String cacheName, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        return RedisManager.getInstance().getCache("players:" + cacheName, type);
    }

    /**
     * Obtiene una caché específica para servidores
     */
    public static <T> RedisCache<T> getServerCache(String cacheName, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        return RedisManager.getInstance().getCache("servers:" + cacheName, type);
    }

    /**
     * Caché inteligente con refresco automático
     */
    public static <T> CompletableFuture<T> smartCache(String key, Class<T> type,
                                                      Supplier<T> dataProvider,
                                                      int cacheTtlSeconds) {
        if (!RedisManager.isAvailable()) {
            return CompletableFuture.supplyAsync(dataProvider);
        }

        return RedisManager.getInstance()
                .getObjectAsync(key, type)
                .thenCompose(cachedData -> {
                    if (cachedData != null) {
                        return CompletableFuture.completedFuture(cachedData);
                    }

                    // Datos no en caché, obtener y cachear
                    return CompletableFuture.supplyAsync(dataProvider)
                            .thenApply(freshData -> {
                                if (freshData != null) {
                                    RedisManager.getInstance().setObject(key, freshData, cacheTtlSeconds);
                                }
                                return freshData;
                            });
                });
    }

    // ==================== UTILIDADES DE PUB/SUB ====================

    /**
     * Envía mensaje cross-server a todos los servidores
     */
    public static void broadcastMessage(String channel, String message) {
        if (!RedisManager.isAvailable()) return;

        RedisManager.getInstance().publish("broadcast:" + channel, message);
    }

    /**
     * Envía objeto cross-server
     */
    public static <T> void broadcastObject(String channel, T object) {
        if (!RedisManager.isAvailable()) return;

        RedisManager.getInstance().publishObject("broadcast:" + channel, object);
    }

    /**
     * Envía mensaje a un servidor específico
     */
    public static void sendToServer(String serverName, String channel, String message) {
        if (!RedisManager.isAvailable()) return;

        String targetChannel = "server:" + serverName + ":" + channel;
        RedisManager.getInstance().publish(targetChannel, message);
    }

    /**
     * Envía notificación a un jugador específico (cross-server)
     */
    public static void sendToPlayer(String playerName, String message) {
        if (!RedisManager.isAvailable()) return;

        String channel = "player:" + playerName.toLowerCase() + ":notifications";
        RedisManager.getInstance().publish(channel, message);
    }

    // ==================== OPERACIONES EN LOTE ====================

    /**
     * Guarda múltiples datos de jugador de una vez
     */
    public static <T> void savePlayerDataBatch(String playerName, Map<String, T> dataMap) {
        savePlayerDataBatch(playerName, dataMap, 3600);
    }

    /**
     * Guarda múltiples datos de jugador con TTL
     */
    public static <T> void savePlayerDataBatch(String playerName, Map<String, T> dataMap, int ttlSeconds) {
        if (!RedisManager.isAvailable() || dataMap == null || dataMap.isEmpty()) return;

        for (Map.Entry<String, T> entry : dataMap.entrySet()) {
            savePlayerData(playerName, entry.getKey(), entry.getValue(), ttlSeconds);
        }
    }

    /**
     * Carga múltiples datos de jugador
     */
    public static <T> Map<String, T> loadPlayerDataBatch(String playerName, Set<String> keys, Class<T> type) {
        Map<String, T> result = new HashMap<>();

        if (!RedisManager.isAvailable() || keys == null || keys.isEmpty()) {
            return result;
        }

        for (String key : keys) {
            T data = loadPlayerData(playerName, key, type);
            if (data != null) {
                result.put(key, data);
            }
        }

        return result;
    }

    // ==================== UTILIDADES DE TIEMPO ====================

    /**
     * Convierte tiempo a segundos para TTL
     */
    public static int toSeconds(long time, TimeUnit unit) {
        return (int) unit.toSeconds(time);
    }

    /**
     * TTL de 1 minuto
     */
    public static int oneMinute() {
        return 60;
    }

    /**
     * TTL de 5 minutos
     */
    public static int fiveMinutes() {
        return 300;
    }

    /**
     * TTL de 1 hora
     */
    public static int oneHour() {
        return 3600;
    }

    /**
     * TTL de 1 día
     */
    public static int oneDay() {
        return 86400;
    }

    /**
     * TTL de 1 semana
     */
    public static int oneWeek() {
        return 604800;
    }

    // ==================== VALIDADORES ====================

    /**
     * Valida que Redis esté disponible para operaciones críticas
     */
    public static void requireRedis() {
        if (!RedisManager.isAvailable()) {
            throw new IllegalStateException("Redis no está disponible para esta operación crítica");
        }
    }

    /**
     * Ejecuta acción solo si Redis está disponible
     */
    public static void ifRedisAvailable(Runnable action) {
        if (RedisManager.isAvailable()) {
            try {
                action.run();
            } catch (Exception e) {
                // Log error but don't throw
                DebugUtils.logInternalError("Error ejecutando acción Redis: " + e.getMessage());
            }
        }
    }

    /**
     * Ejecuta función solo si Redis está disponible, sino devuelve valor por defecto
     */
    public static <T> T ifRedisAvailable(Supplier<T> supplier, T defaultValue) {
        if (RedisManager.isAvailable()) {
            try {
                return supplier.get();
            } catch (Exception e) {
                DebugUtils.logInternalError("Error ejecutando función Redis: " + e.getMessage());
            }
        }
        return defaultValue;
    }

    // ==================== HELPERS PARA BUKKIT ====================

    /**
     * Guarda datos de un jugador de Bukkit
     */
    public static <T> void savePlayerData(Player player, String key, T data) {
        savePlayerData(player.getName(), key, data);
    }

    /**
     * Carga datos de un jugador de Bukkit
     */
    public static <T> T loadPlayerData(Player player, String key, Class<T> type) {
        return loadPlayerData(player.getName(), key, type);
    }

    /**
     * Elimina datos de un jugador de Bukkit
     */
    public static void deletePlayerData(Player player, String key) {
        deletePlayerData(player.getName(), key);
    }

    /**
     * Verifica si un jugador de Bukkit tiene datos
     */
    public static boolean hasPlayerData(Player player, String key) {
        return hasPlayerData(player.getName(), key);
    }

    /**
     * Envía notificación a un jugador de Bukkit
     */
    public static void sendToPlayer(Player player, String message) {
        sendToPlayer(player.getName(), message);
    }
}