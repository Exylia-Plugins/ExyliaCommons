package net.exylia.commons.utils.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SkullManager {

    private static SkullManager instance;
    @Getter
    private static JavaPlugin plugin;
    private static boolean isInitialized = false;

    private static final boolean IS_PAPER = checkPaperSupport();
    private static final String DEFAULT_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19";
    
    private final HttpClient httpClient;

    private final ConcurrentHashMap<String, CachedSkull> textureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedSkull> playerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<ItemStack>> pendingRequests = new ConcurrentHashMap<>();
    
    private static final int MAX_CACHE_SIZE = 100000;
    private static final int MAX_PLAYER_CACHE_SIZE = 100000;
    private static final long CLEANUP_INTERVAL = 480 * 60 * 1000L;
    private static final long RATE_LIMIT_BACKOFF = 60 * 1000L;
    
    private static volatile long backoffUntil = 0L;
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();

    private SkullManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static void initialize(@NotNull JavaPlugin pluginInstance) {
        if (isInitialized) return;
        plugin = pluginInstance;
        instance = new SkullManager();
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
            return createDefaultSkull();
        }

        CachedSkull cached = textureCache.get(base64);
        if (cached != null && !cached.isExpired()) {
            return cached.getSkull().clone();
        }

        ItemStack skull = createSkullFromTextureInternal(base64);

        if (textureCache.size() < MAX_CACHE_SIZE) {
            textureCache.put(base64, new CachedSkull(skull.clone()));
        }

        return skull;
    }

    public ItemStack createSkullFromTextureURL(String url) {
        if (url == null || url.isEmpty()) {
            return createDefaultSkull();
        }

        try {
            new URL(url);
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Invalid texture URL: " + url);
            return createDefaultSkull();
        }

        String base64 = encodeURLToBase64(url);
        return createSkullFromTexture(base64);
    }

    public CompletableFuture<ItemStack> createSkullFromTextureAsync(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return CompletableFuture.completedFuture(createDefaultSkull());
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
                textureCache.put(base64, new CachedSkull(skull.clone()));
            }
            return skull;
        });

        pendingRequests.put(base64, future);
        future.whenComplete((r, t) -> pendingRequests.remove(base64));
        return future;
    }

    public CompletableFuture<ItemStack> createSkullFromTextureURLAsync(String url) {
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture(createDefaultSkull());
        }

        try {
            new URL(url);
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Invalid texture URL: " + url);
            return CompletableFuture.completedFuture(createDefaultSkull());
        }

        String base64 = encodeURLToBase64(url);
        return createSkullFromTextureAsync(base64);
    }

    public CompletableFuture<ItemStack> createPlayerSkullAsync(String playerName) {
        if (playerName == null || playerName.isEmpty() || playerName.contains("%")) {
            return CompletableFuture.completedFuture(createDefaultSkull());
        }
        
        long now = System.currentTimeMillis();
        if (now < backoffUntil) {
            DebugUtils.logInternalDebug("Rate limited, returning default skull for: " + playerName);
            return CompletableFuture.completedFuture(createDefaultSkull());
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
        
        synchronized (requestLock) {
            lastRequestTime.put(key, now);
        }
        
        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> 
            fetchPlayerSkullFromMojang(playerName)
        );
        
        pendingRequests.put(key, future);
        future.whenComplete((r, t) -> pendingRequests.remove(key));
        return future;
    }

    public ItemStack createPlayerSkull(String playerName) {
        if (playerName == null || playerName.isEmpty() || playerName.contains("%")) {
            return createDefaultSkull();
        }
        
        String key = playerName.toLowerCase();
        CachedSkull cached = playerCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.getSkull().clone();
        }
        return createDefaultSkull();
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
                    Schedulers.asyncLater(() -> {
                        createPlayerSkullAsync(name);
                    }, delay / 50L);
                } else {
                    createPlayerSkullAsync(name);
                }
            }
        }
    }

    private ItemStack fetchPlayerSkullFromMojang(String playerName) {
        try {
             
            String uuid = fetchPlayerUUID(playerName);
            if (uuid == null) {
                DebugUtils.logInternalDebug("No UUID found for player: " + playerName);
                return createDefaultSkull();
            }
            
            if ("NOT_FOUND".equals(uuid)) {
                DebugUtils.logInternalDebug("Caching default skull for non-premium player: " + playerName);
                ItemStack defaultSkull = createDefaultSkull();
                String key = playerName.toLowerCase();
                playerCache.put(key, new CachedSkull(defaultSkull.clone()));
                return defaultSkull;
            }
            
            String texture = fetchPlayerTexture(uuid);
            if (texture == null) {
                DebugUtils.logInternalDebug("No texture found for player: " + playerName);
                return createDefaultSkull();
            }
            
            ItemStack skull = createSkullFromTextureInternal(texture);
            
            String key = playerName.toLowerCase();
            playerCache.put(key, new CachedSkull(skull.clone()));
            
            if (skull.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                DebugUtils.logInternalDebug("Caching skull - hasOwner=" + skullMeta.hasOwner());
                DebugUtils.logInternalDebug("Caching skull - owner=" + skullMeta.getOwner());
                DebugUtils.logInternalDebug("Caching skull - profile=" + (skullMeta.getOwnerProfile() != null));
                if (skullMeta.getOwnerProfile() != null) {
                    DebugUtils.logInternalDebug("Caching skull - textures=" + (skullMeta.getOwnerProfile().getTextures() != null));
                }
            }
            
            DebugUtils.logInternalDebug("Successfully fetched skull texture for player: " + playerName);
            return skull;
            
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Failed to fetch skull for player " + playerName + ": " + e.getMessage());
            return createDefaultSkull();
        }
    }

    private ItemStack createDefaultSkull() {
        return createSkullFromTextureInternal(DEFAULT_SKULL_TEXTURE);
    }
    
    private String fetchPlayerUUID(String playerName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 429) {
            DebugUtils.logInternalWarn("Rate limited by Mojang API, backing off for 1 minute");
            synchronized (requestLock) {
                backoffUntil = System.currentTimeMillis() + RATE_LIMIT_BACKOFF;
            }
            return null;
        }
        
        if (response.statusCode() == 404) {
            DebugUtils.logInternalDebug("Player not found (non-premium): " + playerName);
            return "NOT_FOUND";  
        }
        
        if (response.statusCode() != 200) {
            DebugUtils.logInternalDebug("Player not found or API error for: " + playerName + " (Status: " + response.statusCode() + ")");
            return null;
        }
        
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.get("id").getAsString();
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Failed to parse UUID response for player " + playerName + ": " + e.getMessage());
            return null;
        }
    }
    
    private String fetchPlayerTexture(String uuid) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 429) {
            DebugUtils.logInternalWarn("Rate limited by Mojang session server, backing off for 1 minute");
            synchronized (requestLock) {
                backoffUntil = System.currentTimeMillis() + RATE_LIMIT_BACKOFF;
            }
            return null;
        }
        
        if (response.statusCode() != 200) {
            DebugUtils.logInternalDebug("Profile not found or API error for UUID: " + uuid + " (Status: " + response.statusCode() + ")");
            return null;
        }
        
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("properties") && json.get("properties").isJsonArray()) {
                var properties = json.getAsJsonArray("properties");
                for (var property : properties) {
                    var prop = property.getAsJsonObject();
                    if ("textures".equals(prop.get("name").getAsString())) {
                        return prop.get("value").getAsString();
                    }
                }
            }
            DebugUtils.logInternalDebug("No texture property found in profile for UUID: " + uuid);
            return null;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Failed to parse texture response for UUID " + uuid + ": " + e.getMessage());
            return null;
        }
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
            DebugUtils.logInternalWarn("Failed to set skull texture (Paper): " + e.getMessage());
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
            DebugUtils.logInternalWarn("Failed to set skull texture (CraftBukkit): " + e.getMessage());
        }

        return skull;
    }

    private static String encodeURLToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    void startCleanupTask() {
        if (plugin == null) return;

        Schedulers.asyncTimer(() -> {
            textureCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            if (textureCache.size() > MAX_CACHE_SIZE) {
                textureCache.entrySet().stream()
                        .limit(textureCache.size() - MAX_CACHE_SIZE)
                        .forEach(entry -> textureCache.remove(entry.getKey()));
            }

            if (playerCache.size() > MAX_PLAYER_CACHE_SIZE) {
                var entries = new ArrayList<>(playerCache.entrySet());
                entries.stream()
                        .limit(entries.size() - MAX_PLAYER_CACHE_SIZE)
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
                "Cache Stats: Textures=%d, Players=%d, Pending=%d, Backoff=%dms (Mojang API integration)",
                textureCache.size(), playerCache.size(), pendingRequests.size(), backoffRemaining
        );
    }
    
    public boolean isPlayerCached(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        String key = playerName.toLowerCase();
        CachedSkull cached = playerCache.get(key);
        boolean isCached = cached != null && !cached.isExpired();
        return isCached;
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

    public CompletableFuture<List<ItemStack>> createPlayerSkullsBatch(String... playerNames) {
        if (playerNames == null || playerNames.length == 0) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        List<CompletableFuture<ItemStack>> futures = new ArrayList<>();
        
        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name != null && !name.isEmpty()) {
                final int delay = i * 150;  
                
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

        public CachedSkull(ItemStack skull) {
            this.skull = skull;
            this.timestamp = System.currentTimeMillis();
            this.duration = Long.MAX_VALUE;  
        }
        
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
