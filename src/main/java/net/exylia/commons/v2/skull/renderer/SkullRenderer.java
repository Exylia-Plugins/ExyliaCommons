package net.exylia.commons.v2.skull.renderer;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.core.SkullCache;
import net.exylia.commons.v2.skull.core.SkullExecutor;
import net.exylia.commons.v2.skull.fetcher.TextureFetcher;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public class SkullRenderer {

    private final SkullFactory factory;
    private final TextureFetcher textureFetcher;
    private final SkullCache cache;
    private final SkullExecutor executor;
    private final SkullConfig config;

    public SkullRenderer(SkullFactory factory, TextureFetcher textureFetcher, SkullCache cache,
                         SkullExecutor executor, SkullConfig config) {
        this.factory = factory;
        this.textureFetcher = textureFetcher;
        this.cache = cache;
        this.executor = executor;
        this.config = config;
    }

    public ItemStack renderTexture(String base64) {
        if (base64 == null || base64.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Rendering default skull (empty base64)");
            return createDefaultSkull();
        }

        ItemStack cached = cache.getTexture(base64);
        if (cached != null) {
            return cached.clone();
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Rendering new texture skull");
        ItemStack skull = factory.createSkull(base64);
        cache.putTexture(base64, skull.clone());
        return skull;
    }

    public CompletableFuture<ItemStack> renderTextureAsync(String base64) {
        if (base64 == null || base64.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Rendering default skull async (empty base64)");
            return CompletableFuture.completedFuture(createDefaultSkull());
        }

        ItemStack cached = cache.getTexture(base64);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.clone());
        }

        CompletableFuture<ItemStack> pending = cache.getPending(base64);
        if (pending != null && !pending.isDone()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Returning pending texture future");
            return pending;
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Creating new async texture render task");
        CompletableFuture<ItemStack> future = executor.submit(() -> {
            ItemStack skull = factory.createSkull(base64);
            cache.putTexture(base64, skull.clone());
            return skull;
        });

        cache.putPending(base64, future);
        return future;
    }

    public ItemStack renderTextureURL(String url) {
        if (url == null || url.isEmpty()) {
            return createDefaultSkull();
        }

        String base64 = textureFetcher.encodeURLToBase64(url);
        return renderTexture(base64);
    }

    public CompletableFuture<ItemStack> renderTextureURLAsync(String url) {
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture(createDefaultSkull());
        }

        String base64 = textureFetcher.encodeURLToBase64(url);
        return renderTextureAsync(base64);
    }

    public ItemStack renderPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Rendering default skull (empty player name)");
            return createDefaultSkull();
        }

        String key = playerName.toLowerCase();
        ItemStack cached = cache.getPlayer(key);
        if (cached != null) {
            return cached.clone();
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Player skull not cached (sync), returning default: " + playerName);
        return createDefaultSkull();
    }

    public CompletableFuture<ItemStack> renderPlayerAsync(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Rendering default skull async (empty player name)");
            return CompletableFuture.completedFuture(createDefaultSkull());
        }

        String key = playerName.toLowerCase();
        ItemStack cached = cache.getPlayer(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.clone());
        }

        CompletableFuture<ItemStack> pending = cache.getPending(key);
        if (pending != null && !pending.isDone()) {
            DebugAPI.logLibDebug(DebugCategory.SKULL, "Returning pending player future: " + playerName);
            return pending;
        }

        DebugAPI.logLibDebug(DebugCategory.SKULL, "Creating new async player render task: " + playerName);
        CompletableFuture<ItemStack> future = textureFetcher.fetchPlayerTextureAsync(playerName)
                .thenApply(textureOpt -> {
                    String texture = textureOpt.orElse(config.getDefaultTexture());
                    ItemStack skull = factory.createSkull(texture);
                    cache.putPlayer(key, skull.clone());
                    DebugAPI.logLibDebug(DebugCategory.SKULL, "Player skull rendered and cached: " + playerName);
                    return skull;
                });

        cache.putPending(key, future);
        return future;
    }

    private ItemStack createDefaultSkull() {
        return factory.createSkull(config.getDefaultTexture());
    }
}
