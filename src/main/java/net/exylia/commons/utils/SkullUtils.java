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

import static net.exylia.commons.utils.DebugUtils.logWarn;

public class SkullUtils {

    // Cache para evitar reflection repetida
    private static Field cachedProfileField = null;
    private static Method cachedSetPlayerProfileMethod = null;
    private static Class<?> cachedResolvableProfileClass = null;
    private static boolean initializationComplete = false;
    private static boolean useModernMethod = false;

    // Cache para texturas ya procesadas
    private static final ConcurrentHashMap<String, ItemStack> textureCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    static {
        initializeReflection();
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
            // Fallback si la inicialización falló
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
                    logWarn("Failed to set skull profile via reflection: " + e.getMessage());
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
            logWarn("Failed to create head from base64: " + e.getMessage());
        }

        // Fallback final
        head.setItemMeta(headMeta);
        return head;
    }

    /**
     * Encuentra el campo del perfil una sola vez
     */
    private static Field findProfileField(Class<?> metaClass) {
        if (metaClass == null) {
            // Buscar en la clase SkullMeta
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

        // Buscar por nombres conocidos primero
        String[] possibleFieldNames = {"profile", "serializedProfile"};
        for (String fieldName : possibleFieldNames) {
            try {
                return metaClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {}
        }

        // Buscar por tipo
        Field[] fields = metaClass.getDeclaredFields();
        for (Field field : fields) {
            String typeName = field.getType().getSimpleName();
            if (typeName.contains("Profile") || typeName.contains("GameProfile")) {
                return field;
            }
        }

        return null;
    }

    public static ItemStack createHeadFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
        String base64 = Base64.getEncoder().encodeToString(textureJson.getBytes());

        return createHeadFromBase64(base64);
    }

    public static ItemStack createPlayerHead(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        // Verificar cache
        String cacheKey = "player:" + playerName.toLowerCase();
        ItemStack cached = textureCache.get(cacheKey);
        if (cached != null) {
            return cached.clone();
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            headMeta.setOwningPlayer(offlinePlayer);
            head.setItemMeta(headMeta);

            // Agregar al cache
            if (textureCache.size() < MAX_CACHE_SIZE) {
                textureCache.put(cacheKey, head.clone());
            }

        } catch (Exception e) {
            logWarn("Could not set skull owner for player: " + playerName + " - " + e.getMessage());
            head.setItemMeta(headMeta);
        }

        return head;
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
            logWarn("Fallback method also failed: " + e.getMessage());
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    /**
     * Limpia el cache de texturas
     */
    public static void clearCache() {
        textureCache.clear();
    }

    /**
     * Obtiene el tamaño actual del cache
     */
    public static int getCacheSize() {
        return textureCache.size();
    }
}