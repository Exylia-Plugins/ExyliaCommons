package net.exylia.commons.utils.skull;

import org.bukkit.inventory.ItemStack;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class SkullUtils {

    public static ItemStack createSkullFromTexture(String base64) {
        return SkullManager.getInstance().createSkullFromTexture(base64);
    }

    public static ItemStack createSkullFromUrl(String url) {
        return SkullManager.getInstance().createSkullFromTextureURL(url);
    }

    public static ItemStack createPlayerSkull(String playerName) {
        return SkullManager.getInstance().createPlayerSkull(playerName);
    }

    public static CompletableFuture<ItemStack> createPlayerSkullAsync(String playerName) {
        return SkullManager.getInstance().createPlayerSkullAsync(playerName);
    }

    public static void acceptSyncPlayerSkull(String playerName, java.util.function.Consumer<ItemStack> consumer) {
        SkullManager.getInstance().createPlayerSkullAsync(playerName).thenAccept(skull ->
                org.bukkit.Bukkit.getScheduler().runTask(SkullManager.getPlugin(), () -> consumer.accept(skull))
        );
    }

    public static void acceptAsyncPlayerSkull(String playerName, java.util.function.Consumer<ItemStack> consumer) {
        SkullManager.getInstance().createPlayerSkullAsync(playerName).thenAccept(consumer);
    }

    public static String encodeTextureUrl(String url) {
        String fullUrl = url.startsWith("http") ? url : "http://textures.minecraft.net/texture/" + url;
        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", fullUrl);
        return Base64.getEncoder().encodeToString(textureJson.getBytes());
    }

    public static void preloadPlayerSkulls(String... playerNames) {
        SkullManager.getInstance().preloadPlayerSkulls(playerNames);
    }

    public static String getCacheStats() {
        return SkullManager.getInstance().getCacheStats();
    }

    public static void clearCache() {
        SkullManager.getInstance().clearCache();
    }

    public static void clearPlayerCache() {
        SkullManager.getInstance().clearPlayerCache();
    }

    public static void clearTextureCache() {
        SkullManager.getInstance().clearTextureCache();
    }

    public static java.util.concurrent.CompletableFuture<java.util.List<org.bukkit.inventory.ItemStack>> createPlayerSkullsBatch(String... playerNames) {
        return SkullManager.getInstance().createPlayerSkullsBatch(playerNames);
    }
}
