package net.exylia.commons.redis.utils;

import net.exylia.commons.redis.RedisManager;
import net.exylia.commons.redis.cache.RedisCache;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Deprecated
public class RedisUtils {

    private static final String PLAYER_PREFIX = "player:";
    private static final String SERVER_PREFIX = "server:";
    private static final String GLOBAL_PREFIX = "global:";

    public static <T> void savePlayerData(String playerName, String key, T data) {
        savePlayerData(playerName, key, data, 3600);  
    }

    public static <T> void savePlayerData(String playerName, String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
    }

    public static <T> T loadPlayerData(String playerName, String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    public static <T> T loadPlayerDataOrDefault(String playerName, String key, Class<T> type, T defaultValue) {
        T data = loadPlayerData(playerName, key, type);
        return data != null ? data : defaultValue;
    }

    public static void deletePlayerData(String playerName, String key) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        RedisManager.getInstance().delete(redisKey);
    }

    public static boolean hasPlayerData(String playerName, String key) {
        if (!RedisManager.isAvailable()) return false;

        String redisKey = PLAYER_PREFIX + playerName.toLowerCase() + ":" + key;
        return RedisManager.getInstance().exists(redisKey);
    }

    public static <T> void saveServerData(String serverName, String key, T data) {
        saveServerData(serverName, key, data, -1);  
    }

    public static <T> void saveServerData(String serverName, String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = SERVER_PREFIX + serverName + ":" + key;
        if (ttlSeconds > 0) {
            RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
        } else {
            RedisManager.getInstance().setObject(redisKey, data);
        }
    }

    public static <T> T loadServerData(String serverName, String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = SERVER_PREFIX + serverName + ":" + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    public static <T> void saveGlobalData(String key, T data) {
        saveGlobalData(key, data, -1);
    }

    public static <T> void saveGlobalData(String key, T data, int ttlSeconds) {
        if (!RedisManager.isAvailable()) return;

        String redisKey = GLOBAL_PREFIX + key;
        if (ttlSeconds > 0) {
            RedisManager.getInstance().setObject(redisKey, data, ttlSeconds);
        } else {
            RedisManager.getInstance().setObject(redisKey, data);
        }
    }

    public static <T> T loadGlobalData(String key, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        String redisKey = GLOBAL_PREFIX + key;
        return RedisManager.getInstance().getObject(redisKey, type);
    }

    public static <T> RedisCache<T> getPlayerCache(String cacheName, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        return RedisManager.getInstance().getCache("players:" + cacheName, type);
    }

    public static <T> RedisCache<T> getServerCache(String cacheName, Class<T> type) {
        if (!RedisManager.isAvailable()) return null;

        return RedisManager.getInstance().getCache("servers:" + cacheName, type);
    }

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

                    return CompletableFuture.supplyAsync(dataProvider)
                            .thenApply(freshData -> {
                                if (freshData != null) {
                                    RedisManager.getInstance().setObject(key, freshData, cacheTtlSeconds);
                                }
                                return freshData;
                            });
                });
    }

    public static void broadcastMessage(String channel, String message) {
        if (!RedisManager.isAvailable()) return;

        RedisManager.getInstance().publish("broadcast:" + channel, message);
    }

    public static <T> void broadcastObject(String channel, T object) {
        if (!RedisManager.isAvailable()) return;

        RedisManager.getInstance().publishObject("broadcast:" + channel, object);
    }

    public static void sendToServer(String serverName, String channel, String message) {
        if (!RedisManager.isAvailable()) return;

        String targetChannel = "server:" + serverName + ":" + channel;
        RedisManager.getInstance().publish(targetChannel, message);
    }

    public static void sendToPlayer(String playerName, String message) {
        if (!RedisManager.isAvailable()) return;

        String channel = "player:" + playerName.toLowerCase() + ":notifications";
        RedisManager.getInstance().publish(channel, message);
    }

    public static <T> void savePlayerDataBatch(String playerName, Map<String, T> dataMap) {
        savePlayerDataBatch(playerName, dataMap, 3600);
    }

    public static <T> void savePlayerDataBatch(String playerName, Map<String, T> dataMap, int ttlSeconds) {
        if (!RedisManager.isAvailable() || dataMap == null || dataMap.isEmpty()) return;

        for (Map.Entry<String, T> entry : dataMap.entrySet()) {
            savePlayerData(playerName, entry.getKey(), entry.getValue(), ttlSeconds);
        }
    }

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

    public static int toSeconds(long time, TimeUnit unit) {
        return (int) unit.toSeconds(time);
    }

    public static int oneMinute() {
        return 60;
    }

    public static int fiveMinutes() {
        return 300;
    }

    public static int oneHour() {
        return 3600;
    }

    public static int oneDay() {
        return 86400;
    }

    public static int oneWeek() {
        return 604800;
    }

    public static void requireRedis() {
        if (!RedisManager.isAvailable()) {
            throw new IllegalStateException("Redis no está disponible para esta operación crítica");
        }
    }

    public static void ifRedisAvailable(Runnable action) {
        if (RedisManager.isAvailable()) {
            try {
                action.run();
            } catch (Exception e) {
                 
                DebugUtils.logInternalError("Error ejecutando acción Redis: " + e.getMessage());
            }
        }
    }

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

    public static <T> void savePlayerData(Player player, String key, T data) {
        savePlayerData(player.getName(), key, data);
    }

    public static <T> T loadPlayerData(Player player, String key, Class<T> type) {
        return loadPlayerData(player.getName(), key, type);
    }

    public static void deletePlayerData(Player player, String key) {
        deletePlayerData(player.getName(), key);
    }

    public static boolean hasPlayerData(Player player, String key) {
        return hasPlayerData(player.getName(), key);
    }

    public static void sendToPlayer(Player player, String message) {
        sendToPlayer(player.getName(), message);
    }
}
