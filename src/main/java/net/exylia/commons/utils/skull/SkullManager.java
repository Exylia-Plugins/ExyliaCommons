package net.exylia.commons.utils.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.rollczi.liteskullapi.LiteSkullAPI;
import dev.rollczi.liteskullapi.LiteSkullFactory;
import dev.rollczi.liteskullapi.SkullAPI;
import lombok.Getter;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class SkullManager {

    private static SkullManager instance;
    @Getter
    private static JavaPlugin plugin;
    private static boolean isInitialized = false;

    private static final String BUKKIT_VERSION = Bukkit.getVersion();
    private static final boolean IS_PAPER = checkPaperSupport();
    private static final boolean IS_MODERN_VERSION = isModernVersion();

    @Getter
    private SkullAPI skullAPI;

    private final ConcurrentHashMap<String, CachedSkull> textureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<ItemStack>> pendingRequests = new ConcurrentHashMap<>();

    private static final int MAX_CACHE_SIZE = 1000;
    private static final long TEXTURE_CACHE_DURATION = TimeUnit.HOURS.toMillis(24);
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(30);
    private final ConcurrentHashMap<String, CachedSkull> playerCache = new ConcurrentHashMap<>();
    private static volatile long backoffUntil = 0L;
    private static final long PLAYER_CACHE_DURATION = TimeUnit.HOURS.toMillis(6);
    private static final int MAX_PLAYER_CACHE_SIZE = 2000;
    private static final long BACKOFF_ON_429 = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    private static final long REQUEST_DELAY = 100L;
    
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();

    private SkullManager() {}

    public static void initialize(@NotNull JavaPlugin pluginInstance) {
        if (isInitialized) return;
        plugin = pluginInstance;
        instance = new SkullManager();

        if (IS_PAPER) {
            DebugUtils.logInternalInfo("Paper detected for texture skulls");
        } else {
            logInternalWarn("Paper not detected; falling back to CraftBukkit reflection for texture skulls");
        }

        try {
            instance.skullAPI = LiteSkullFactory.builder()
                    .cacheExpireAfterWrite(Duration.ofMinutes(45L))
                    .bukkitScheduler(pluginInstance)
                    .build();
            DebugUtils.logInternalInfo("LiteSkullAPI initialized successfully for player skulls");
        } catch (Exception e) {
            logInternalWarn("Failed to initialize LiteSkullAPI, falling back to basic player skulls: " + e.getMessage());
            instance.skullAPI = null;
        }

        instance.startCleanupTask();
        isInitialized = true;
    }

    public static SkullManager getInstance() {
        if (!isInitialized) {
            throw new IllegalStateException("SkullManager not initialized! Call initialize(plugin) first.");
        }
        return instance;
    }

    public ItemStack createSkullFromTexture(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        CachedSkull cached = textureCache.get(base64);
        if (cached != null && !cached.isExpired()) {
            return cached.getSkull().clone();
        }

        ItemStack skull = createSkullFromTextureInternal(base64);

        if (textureCache.size() < MAX_CACHE_SIZE) {
            textureCache.put(base64, new CachedSkull(skull.clone(), TEXTURE_CACHE_DURATION));
        }

        return skull;
    }

    public ItemStack createSkullFromTextureURL(String url) {
        if (url == null || url.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            logInternalWarn("Invalid texture URL: " + url);
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String base64 = encodeURLToBase64(url);
        return createSkullFromTexture(base64);
    }

    public CompletableFuture<ItemStack> createSkullFromTextureAsync(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
        }

        CachedSkull cached = textureCache.get(base64);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.getSkull().clone());
        }

        CompletableFuture<ItemStack> pending = pendingRequests.get(base64);
        if (pending != null && !pending.isDone()) {
            return pending;
        }

        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> {
            ItemStack skull = createSkullFromTextureInternal(base64);
            if (textureCache.size() < MAX_CACHE_SIZE) {
                textureCache.put(base64, new CachedSkull(skull.clone(), TEXTURE_CACHE_DURATION));
            }
            return skull;
        });

        pendingRequests.put(base64, future);
        future.whenComplete((r, t) -> pendingRequests.remove(base64));
        return future;
    }

    public CompletableFuture<ItemStack> createSkullFromTextureURLAsync(String url) {
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            logInternalWarn("Invalid texture URL: " + url);
            return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
        }

        String base64 = encodeURLToBase64(url);
        return createSkullFromTextureAsync(base64);
    }

    public CompletableFuture<ItemStack> createPlayerSkullAsync(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
        }
        
        long now = System.currentTimeMillis();
        if (now < backoffUntil) {
            return CompletableFuture.completedFuture(createBasicPlayerSkull(playerName));
        }
        
        String key = playerName.toLowerCase();
        CachedSkull cached = playerCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.getSkull().clone());
        }
        
        CompletableFuture<ItemStack> pending = pendingRequests.get(key);
        if (pending != null && !pending.isDone()) {
            return pending;
        }
        
        // Rate limiting check
        synchronized (requestLock) {
            Long lastRequest = lastRequestTime.get(key);
            if (lastRequest != null && (now - lastRequest) < REQUEST_DELAY) {
                return CompletableFuture.completedFuture(createBasicPlayerSkull(playerName));
            }
            
            // Limit concurrent requests
            if (pendingRequests.size() >= MAX_CONCURRENT_REQUESTS) {
                return CompletableFuture.completedFuture(createBasicPlayerSkull(playerName));
            }
            
            lastRequestTime.put(key, now);
        }
        
        if (skullAPI == null) {
            CompletableFuture<ItemStack> f = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(REQUEST_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return createPlayerSkullInternal(playerName);
            });
            pendingRequests.put(key, f);
            f.whenComplete((r, t) -> pendingRequests.remove(key));
            return f;
        }
        
        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }).thenCompose(ignored -> 
            skullAPI.getSkull(playerName)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        String msg = String.valueOf(throwable);
                        if (msg.contains("Status: 429") || msg.contains("Too Many Requests")) {
                            synchronized (requestLock) {
                                backoffUntil = System.currentTimeMillis() + BACKOFF_ON_429;
                                DebugUtils.logInternalWarn("Rate limited by Mojang API, backing off for " + (BACKOFF_ON_429 / 1000) + " seconds");
                            }
                        }
                        return createBasicPlayerSkull(playerName);
                    }
                    playerCache.put(key, new CachedSkull(result.clone(), PLAYER_CACHE_DURATION));
                    if (playerCache.size() > MAX_PLAYER_CACHE_SIZE) {
                        playerCache.entrySet().stream()
                                .limit(playerCache.size() - MAX_PLAYER_CACHE_SIZE)
                                .forEach(e -> playerCache.remove(e.getKey()));
                    }
                    return result;
                })
        );
        
        pendingRequests.put(key, future);
        future.whenComplete((r, t) -> pendingRequests.remove(key));
        return future;
    }

    public ItemStack createPlayerSkull(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        return createBasicPlayerSkull(playerName);
    }

    public void preloadPlayerSkulls(String... playerNames) {
        if (playerNames == null) return;
        long now = System.currentTimeMillis();
        if (now < backoffUntil) return;
        
        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name != null && !name.isEmpty()) {
                int delay = i * 50;
                if (plugin != null) {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        createPlayerSkullAsync(name);
                    }, delay / 50L);
                } else {
                    createPlayerSkullAsync(name);
                }
            }
        }
    }

    private ItemStack createPlayerSkullInternal(String playerName) {
        try {
            if (skullAPI != null) {
                return skullAPI.getSkull(playerName).join();
            }
        } catch (Exception ignored) {}
        return createBasicPlayerSkull(playerName);
    }

    private ItemStack createBasicPlayerSkull(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            if (plugin != null) {
                logInternalWarn("Failed to create basic player skull: " + e.getMessage());
            }
        }

        return skull;
    }

    private ItemStack createSkullFromTextureInternal(String base64Texture) {
        if (IS_PAPER) {
            return createPaperSkull(base64Texture);
        } else {
            return createCraftBukkitSkull(base64Texture);
        }
    }

    private ItemStack createPaperSkull(String base64Texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            logInternalWarn("Failed to set skull texture (Paper): " + e.getMessage());
        }

        return skull;
    }

    private ItemStack createCraftBukkitSkull(String base64Texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64Texture));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            logInternalWarn("Failed to set skull texture (CraftBukkit): " + e.getMessage());
        }

        return skull;
    }

    private static String encodeURLToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    void startCleanupTask() {
        if (plugin == null) return;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            textureCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            playerCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

            if (textureCache.size() > MAX_CACHE_SIZE) {
                textureCache.entrySet().stream()
                        .limit(textureCache.size() - MAX_CACHE_SIZE)
                        .forEach(entry -> textureCache.remove(entry.getKey()));
            }
            if (playerCache.size() > MAX_PLAYER_CACHE_SIZE) {
                playerCache.entrySet().stream()
                        .limit(playerCache.size() - MAX_PLAYER_CACHE_SIZE)
                        .forEach(entry -> playerCache.remove(entry.getKey()));
            }
        }, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50);
    }

    public void clearCache() {
        textureCache.clear();
        pendingRequests.clear();
        lastRequestTime.clear();
    }

    public void clearPlayerCache() {
        pendingRequests.clear();
        playerCache.clear();
        lastRequestTime.clear();
    }

    public void clearTextureCache() {
        textureCache.clear();
    }

    public String getCacheStats() {
        long backoffRemaining = Math.max(0, backoffUntil - System.currentTimeMillis());
        return String.format(
                "Cache Stats: Textures=%d, Players=%d, Pending=%d, Backoff=%dms (Player cache: local + LiteSkullAPI)",
                textureCache.size(), playerCache.size(), pendingRequests.size(), backoffRemaining
        );
    }

    private static boolean checkPaperSupport() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isModernVersion() {
        try {
            String version = BUKKIT_VERSION;
            if (version.contains("1.20") || version.contains("1.21") || version.contains("1.22")) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public CompletableFuture<List<ItemStack>> createPlayerSkullsBatch(String... playerNames) {
        if (playerNames == null || playerNames.length == 0) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        List<CompletableFuture<ItemStack>> futures = new ArrayList<>();
        
        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name != null && !name.isEmpty()) {
                final int delay = i * 100;
                
                CompletableFuture<ItemStack> delayed = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }).thenCompose(ignored -> createPlayerSkullAsync(name));
                
                futures.add(delayed);
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(java.util.stream.Collectors.toList()));
    }

    public static class CachedSkull {
        @Getter
        private final ItemStack skull;
        private final long timestamp;
        private final long duration;

        public CachedSkull(ItemStack skull, long duration) {
            this.skull = skull;
            this.timestamp = System.currentTimeMillis();
            this.duration = duration;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > duration;
        }
    }
}