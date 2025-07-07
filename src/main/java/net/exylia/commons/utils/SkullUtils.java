package net.exylia.commons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class SkullUtils {

    // Cache para evitar reflection repetida
    private static Field cachedProfileField = null;
    private static Method cachedSetPlayerProfileMethod = null;
    private static Class<?> cachedResolvableProfileClass = null;
    private static boolean initializationComplete = false;
    private static boolean useModernMethod = false;

    // Cache mejorado para texturas y jugadores
    private static final ConcurrentHashMap<String, ItemStack> textureCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<ItemStack>> pendingRequests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> playerCacheTimestamps = new ConcurrentHashMap<>();

    // Configuración del cache
    private static final int MAX_CACHE_SIZE = 500; // Aumentado para más items
    private static final long PLAYER_CACHE_DURATION = TimeUnit.HOURS.toMillis(1); // 1 hora
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(30); // 30 minutos

    // Cleanup task
    private static boolean cleanupTaskStarted = false;

    static {
        initializeReflection();
        startCleanupTask();
    }

    /**
     * Inicializa la reflexión una sola vez al cargar la clase
     */
    private static void initializeReflection() {
        try {
            cachedResolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            cachedProfileField = findProfileField(null);
            if (cachedProfileField != null) {
                cachedProfileField.setAccessible(true);
                useModernMethod = true;
                initializationComplete = true;
                return;
            }
        } catch (Exception ignored) {}

        try {
            cachedProfileField = findProfileField(null);
            if (cachedProfileField != null) {
                cachedProfileField.setAccessible(true);
                useModernMethod = false;
                initializationComplete = true;
                return;
            }
        } catch (Exception ignored) {}

        try {
            Class<?> skullMetaClass = Class.forName("org.bukkit.inventory.meta.SkullMeta");
            cachedSetPlayerProfileMethod = skullMetaClass.getMethod("setPlayerProfile", GameProfile.class);
            initializationComplete = true;
        } catch (Exception ignored) {}
    }

    /**
     * Inicia la tarea de limpieza del cache
     */
    private static void startCleanupTask() {
        if (cleanupTaskStarted) return;

        cleanupTaskStarted = true;
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0], // Usar cualquier plugin disponible
                SkullUtils::cleanupCache,
                20L * 60 * 30, // Cada 30 minutos
                20L * 60 * 30
        );
    }

    /**
     * Limpia el cache de entradas antiguas
     */
    private static void cleanupCache() {
        long currentTime = System.currentTimeMillis();

        // Limpiar cache de jugadores antiguos
        playerCacheTimestamps.entrySet().removeIf(entry -> {
            boolean expired = currentTime - entry.getValue() > PLAYER_CACHE_DURATION;
            if (expired) {
                String playerKey = "player:" + entry.getKey().toLowerCase();
                textureCache.remove(playerKey);
            }
            return expired;
        });

        // Si el cache es muy grande, remover entradas más antiguas
        if (textureCache.size() > MAX_CACHE_SIZE) {
            int toRemove = textureCache.size() - MAX_CACHE_SIZE;
            textureCache.entrySet().stream()
                    .limit(toRemove)
                    .map(entry -> entry.getKey())
                    .forEach(textureCache::remove);
        }
    }

    public static ItemStack createHeadFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        // Verificar cache primero
        ItemStack cached = textureCache.get(base64);
        if (cached != null) {
            return cached.clone();
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        if (!initializationComplete) {
            return createHeadFromBase64Fallback(base64);
        }

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
            profile.getProperties().put("textures", new Property("textures", base64));

            boolean success = false;

            if (cachedSetPlayerProfileMethod != null) {
                try {
                    cachedSetPlayerProfileMethod.invoke(headMeta, profile);
                    success = true;
                } catch (Exception ignored) {}
            }

            if (!success && cachedProfileField != null) {
                try {
                    if (useModernMethod && cachedResolvableProfileClass != null) {
                        Object resolvableProfile = cachedResolvableProfileClass.getConstructor(GameProfile.class).newInstance(profile);
                        cachedProfileField.set(headMeta, resolvableProfile);
                    } else {
                        cachedProfileField.set(headMeta, profile);
                    }
                    success = true;
                } catch (Exception e) {
                    logInternalWarn("Failed to set skull profile via reflection: " + e.getMessage());
                }
            }

            if (success) {
                head.setItemMeta(headMeta);

                // Agregar al cache si no está lleno
                if (textureCache.size() < MAX_CACHE_SIZE) {
                    textureCache.put(base64, head.clone());
                }

                return head;
            }

        } catch (Exception e) {
            logInternalWarn("Failed to create head from base64: " + e.getMessage());
        }

        head.setItemMeta(headMeta);
        return head;
    }

    /**
     * Crea una cabeza de jugador con cache inteligente
     * OPTIMIZACIÓN PRINCIPAL: Evita llamadas innecesarias a Mojang
     */
    public static ItemStack createPlayerHead(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String cacheKey = "player:" + playerName.toLowerCase();

        // Verificar cache con timestamp
        ItemStack cached = textureCache.get(cacheKey);
        Long cacheTime = playerCacheTimestamps.get(playerName.toLowerCase());

        if (cached != null && cacheTime != null) {
            long age = System.currentTimeMillis() - cacheTime;
            if (age < PLAYER_CACHE_DURATION) {
                return cached.clone();
            }
        }

        // Verificar si ya hay una petición pendiente para este jugador
        CompletableFuture<ItemStack> pending = pendingRequests.get(cacheKey);
        if (pending != null && !pending.isDone()) {
            // Si hay una petición pendiente, devolver una cabeza básica por ahora
            return createBasicPlayerHead(playerName);
        }

        // Crear nueva petición asíncrona
        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> createPlayerHeadSync(playerName));

        pendingRequests.put(cacheKey, future);

        // Limpiar la petición pendiente cuando termine
        future.whenComplete((result, throwable) -> {
            pendingRequests.remove(cacheKey);
            if (result != null && throwable == null) {
                textureCache.put(cacheKey, result.clone());
                playerCacheTimestamps.put(playerName.toLowerCase(), System.currentTimeMillis());
            }
        });

        // Devolver una cabeza básica inmediatamente para no bloquear
        return createBasicPlayerHead(playerName);
    }

    /**
     * Crea una cabeza básica con el nombre del jugador (sin textura)
     */
    private static ItemStack createBasicPlayerHead(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        try {
            // Crear un perfil simple sin propiedades de textura
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(playerName.getBytes()), playerName);

            if (cachedSetPlayerProfileMethod != null) {
                cachedSetPlayerProfileMethod.invoke(headMeta, profile);
            } else if (cachedProfileField != null) {
                if (useModernMethod && cachedResolvableProfileClass != null) {
                    Object resolvableProfile = cachedResolvableProfileClass.getConstructor(GameProfile.class).newInstance(profile);
                    cachedProfileField.set(headMeta, resolvableProfile);
                } else {
                    cachedProfileField.set(headMeta, profile);
                }
            }

            head.setItemMeta(headMeta);
        } catch (Exception e) {
            logInternalWarn("Failed to create basic player head: " + e.getMessage());
        }

        return head;
    }

    /**
     * Versión síncrona para crear cabeza de jugador (solo usar en threads separados)
     */
    private static ItemStack createPlayerHeadSync(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            headMeta.setOwningPlayer(offlinePlayer);
            head.setItemMeta(headMeta);
        } catch (Exception e) {
            logInternalWarn("Could not set skull owner for player: " + playerName + " - " + e.getMessage());
            head.setItemMeta(headMeta);
        }

        return head;
    }

    /**
     * Para pre-cargar cabezas de jugadores de forma asíncrona
     * Úsalo al inicio del servidor o cuando sepas qué jugadores necesitarás
     */
    public static void preloadPlayerHeads(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName == null || playerName.isEmpty()) continue;

            String cacheKey = "player:" + playerName.toLowerCase();

            // Solo pre-cargar si no está en cache o está expirado
            ItemStack cached = textureCache.get(cacheKey);
            Long cacheTime = playerCacheTimestamps.get(playerName.toLowerCase());

            if (cached == null || cacheTime == null ||
                    (System.currentTimeMillis() - cacheTime) > PLAYER_CACHE_DURATION) {

                // Crear petición asíncrona sin bloquear
                CompletableFuture.supplyAsync(() -> createPlayerHeadSync(playerName))
                        .thenAccept(head -> {
                            textureCache.put(cacheKey, head.clone());
                            playerCacheTimestamps.put(playerName.toLowerCase(), System.currentTimeMillis());
                        });
            }
        }
    }

    /**
     * Versión optimizada que permite especificar si usar cache o forzar actualización
     */
    public static ItemStack createPlayerHead(String playerName, boolean forceRefresh) {
        if (!forceRefresh) {
            return createPlayerHead(playerName);
        }

        // Limpiar cache para este jugador
        String cacheKey = "player:" + playerName.toLowerCase();
        textureCache.remove(cacheKey);
        playerCacheTimestamps.remove(playerName.toLowerCase());

        return createPlayerHead(playerName);
    }

    public static ItemStack createHeadFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
        String base64 = Base64.getEncoder().encodeToString(textureJson.getBytes());

        return createHeadFromBase64(base64);
    }

    public static String getEncodedTexture(String url) {
        String fullUrl = url.startsWith("http") ? url : "http://textures.minecraft.net/texture/" + url;
        String json = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", fullUrl);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    public static ItemStack createHeadFromBase64Fallback(String base64) {
        try {
            UUID textureUUID = UUID.nameUUIDFromBytes(base64.getBytes());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(textureUUID);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                head.setItemMeta(meta);
            }

            return head;
        } catch (Exception e) {
            logInternalWarn("Fallback method also failed: " + e.getMessage());
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    /**
     * Encuentra el campo del perfil una sola vez
     */
    private static Field findProfileField(Class<?> metaClass) {
        if (metaClass == null) {
            try {
                metaClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftMetaSkull");
            } catch (Exception e) {
                try {
                    metaClass = Class.forName("org.bukkit.inventory.meta.SkullMeta");
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        String[] possibleFieldNames = {"profile", "serializedProfile"};
        for (String fieldName : possibleFieldNames) {
            try {
                return metaClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {}
        }

        Field[] fields = metaClass.getDeclaredFields();
        for (Field field : fields) {
            String typeName = field.getType().getSimpleName();
            if (typeName.contains("Profile") || typeName.contains("GameProfile")) {
                return field;
            }
        }

        return null;
    }

    /**
     * Limpia el cache de texturas
     */
    public static void clearCache() {
        textureCache.clear();
        playerCacheTimestamps.clear();
        pendingRequests.clear();
    }

    /**
     * Limpia solo el cache de jugadores
     */
    public static void clearPlayerCache() {
        textureCache.entrySet().removeIf(entry -> entry.getKey().startsWith("player:"));
        playerCacheTimestamps.clear();
    }

    /**
     * Obtiene el tamaño actual del cache
     */
    public static int getCacheSize() {
        return textureCache.size();
    }

    /**
     * Obtiene estadísticas del cache
     */
    public static String getCacheStats() {
        int totalItems = textureCache.size();
        int playerItems = (int) textureCache.keySet().stream().filter(key -> key.startsWith("player:")).count();
        int textureItems = totalItems - playerItems;
        int pendingRequests = SkullUtils.pendingRequests.size();

        return String.format("Cache Stats: Total=%d, Players=%d, Textures=%d, Pending=%d",
                totalItems, playerItems, textureItems, pendingRequests);
    }
}