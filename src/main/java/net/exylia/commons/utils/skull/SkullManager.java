package net.exylia.commons.utils.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SkullManager {

    private static SkullManager instance;
    private static JavaPlugin plugin;
    private static boolean isInitialized = false;

    // Version detection
    private static final String BUKKIT_VERSION = Bukkit.getVersion();
    private static final boolean IS_PAPER = checkPaperSupport();
    private static final boolean IS_MODERN_VERSION = isModernVersion();

    // Cache system
    private final ConcurrentHashMap<String, CachedSkull> textureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedSkull> playerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<ItemStack>> pendingRequests = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long PLAYER_CACHE_DURATION = TimeUnit.HOURS.toMillis(1);
    private static final long TEXTURE_CACHE_DURATION = TimeUnit.HOURS.toMillis(24);
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(30);

    private SkullManager() {}

    /**
     * Initialize the skull manager with a plugin instance
     */
    public static void initialize(JavaPlugin pluginInstance) {
        if (isInitialized) return;

        plugin = pluginInstance;
        instance = new SkullManager();
        instance.startCleanupTask();
        isInitialized = true;

        plugin.getLogger().info("SkullManager initialized for version: " + BUKKIT_VERSION);
        plugin.getLogger().info("Paper support: " + IS_PAPER + ", Modern version: " + IS_MODERN_VERSION);
    }

    /**
     * Get the singleton instance
     */
    public static SkullManager getInstance() {
        if (!isInitialized) {
            throw new IllegalStateException("SkullManager not initialized! Call initialize(plugin) first.");
        }
        return instance;
    }

    /**
     * Create a skull from base64 texture data
     */
    public ItemStack createSkullFromTexture(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        // Check cache first
        CachedSkull cached = textureCache.get(base64);
        if (cached != null && !cached.isExpired()) {
            return cached.getSkull().clone();
        }

        ItemStack skull = createSkullFromTextureInternal(base64);

        // Cache the result
        if (textureCache.size() < MAX_CACHE_SIZE) {
            textureCache.put(base64, new CachedSkull(skull.clone(), TEXTURE_CACHE_DURATION));
        }

        return skull;
    }

    /**
     * Create a skull from a texture URL
     */
    public ItemStack createSkullFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String fullUrl = url.startsWith("http") ? url : "http://textures.minecraft.net/texture/" + url;
        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", fullUrl);
        String base64 = Base64.getEncoder().encodeToString(textureJson.getBytes());

        return createSkullFromTexture(base64);
    }

    /**
     * Create a player skull asynchronously
     */
    public CompletableFuture<ItemStack> createPlayerSkullAsync(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
        }

        String cacheKey = playerName.toLowerCase();

        // Check cache first
        CachedSkull cached = playerCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.getSkull().clone());
        }

        // Check if there's already a pending request
        CompletableFuture<ItemStack> pending = pendingRequests.get(cacheKey);
        if (pending != null && !pending.isDone()) {
            return pending;
        }

        // Create new async request
        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> {
            ItemStack skull = createPlayerSkullInternal(playerName);

            // Cache the result
            if (playerCache.size() < MAX_CACHE_SIZE) {
                playerCache.put(cacheKey, new CachedSkull(skull.clone(), PLAYER_CACHE_DURATION));
            }

            return skull;
        });

        pendingRequests.put(cacheKey, future);

        // Clean up when done
        future.whenComplete((result, throwable) -> {
            pendingRequests.remove(cacheKey);
        });

        return future;
    }

    /**
     * Create a player skull synchronously (returns basic skull immediately)
     */
    public ItemStack createPlayerSkull(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String cacheKey = playerName.toLowerCase();

        // Check cache first
        CachedSkull cached = playerCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getSkull().clone();
        }

        // Start async loading for future use
        createPlayerSkullAsync(playerName);

        // Return basic skull immediately
        return createBasicPlayerSkull(playerName);
    }

    /**
     * Preload player skulls asynchronously
     */
    public void preloadPlayerSkulls(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName != null && !playerName.isEmpty()) {
                createPlayerSkullAsync(playerName);
            }
        }
    }

    /**
     * Internal method to create skull from base64 texture
     */
    private ItemStack createSkullFromTextureInternal(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            if (IS_PAPER && IS_MODERN_VERSION) {
                // Paper 1.20.5+ method
                setSkullTexturePaperModern(meta, base64);
            } else if (IS_PAPER) {
                // Paper 1.16.5 - 1.20.4 method
                setSkullTexturePaperLegacy(meta, base64);
            } else {
                // Spigot fallback method
                setSkullTextureSpigot(meta, base64);
            }

            skull.setItemMeta(meta);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create skull from texture: " + e.getMessage());
            }
        }

        return skull;
    }

    /**
     * Internal method to create player skull
     */
    private ItemStack createPlayerSkullInternal(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create player skull for " + playerName + ": " + e.getMessage());
            }
        }

        return skull;
    }

    /**
     * Create a basic player skull without texture
     */
    private ItemStack createBasicPlayerSkull(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            // Create a simple offline player
            UUID uuid = UUID.nameUUIDFromBytes(playerName.getBytes());
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to create basic player skull: " + e.getMessage());
            }
        }

        return skull;
    }

    /**
     * Set skull texture for Paper 1.20.5+
     */
    private void setSkullTexturePaperModern(SkullMeta meta, String base64) {
        try {
            UUID uuid = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(uuid, uuid.toString().substring(0, 16));
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set skull texture (Paper Modern)", e);
        }
    }

    /**
     * Set skull texture for Paper 1.16.5 - 1.20.4
     */
    private void setSkullTexturePaperLegacy(SkullMeta meta, String base64) {
        try {
            UUID uuid = UUID.randomUUID();
            PlayerProfile profile = Bukkit.createProfile(uuid, null);
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            // Fallback to Spigot method
            setSkullTextureSpigot(meta, base64);
        }
    }

    /**
     * Set skull texture for Spigot (reflection fallback)
     */
    private void setSkullTextureSpigot(SkullMeta meta, String base64) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", base64));

            // Try to set profile using reflection
            try {
                java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (Exception e) {
                // If reflection fails, try with ResolvableProfile for newer versions
                try {
                    Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
                    Object resolvableProfile = resolvableProfileClass.getConstructor(GameProfile.class).newInstance(profile);

                    java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, resolvableProfile);
                } catch (Exception e2) {
                    throw new RuntimeException("Failed to set skull texture (Spigot)", e2);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set skull texture (Spigot)", e);
        }
    }

    /**
     * Check if Paper is available
     */
    private static boolean checkPaperSupport() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if this is a modern version (1.20.5+)
     */
    private static boolean isModernVersion() {
        try {
            // Try to access a class that was introduced in 1.20.5+
            Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Start the cleanup task
     */
    private void startCleanupTask() {
        if (plugin == null) return;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Clean expired entries
            textureCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            playerCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

            // Clean up if cache is too large
            if (textureCache.size() > MAX_CACHE_SIZE) {
                textureCache.entrySet().stream()
                        .limit(textureCache.size() - MAX_CACHE_SIZE)
                        .forEach(entry -> textureCache.remove(entry.getKey()));
            }

            if (playerCache.size() > MAX_CACHE_SIZE) {
                playerCache.entrySet().stream()
                        .limit(playerCache.size() - MAX_CACHE_SIZE)
                        .forEach(entry -> playerCache.remove(entry.getKey()));
            }
        }, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50); // Convert to ticks
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("Cache Stats: Textures=%d, Players=%d, Pending=%d",
                textureCache.size(), playerCache.size(), pendingRequests.size());
    }

    /**
     * Clear all caches
     */
    public void clearCache() {
        textureCache.clear();
        playerCache.clear();
        pendingRequests.clear();
    }

    /**
     * Clear only player cache
     */
    public void clearPlayerCache() {
        playerCache.clear();
    }

    /**
     * Clear only texture cache
     */
    public void clearTextureCache() {
        textureCache.clear();
    }

    /**
     * Cached skull wrapper
     */
    private static class CachedSkull {
        private final ItemStack skull;
        private final long timestamp;
        private final long duration;

        public CachedSkull(ItemStack skull, long duration) {
            this.skull = skull;
            this.timestamp = System.currentTimeMillis();
            this.duration = duration;
        }

        public ItemStack getSkull() {
            return skull;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > duration;
        }
    }
}