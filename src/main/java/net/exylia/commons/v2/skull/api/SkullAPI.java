package net.exylia.commons.v2.skull.api;

import net.exylia.commons.v2.skull.builder.SkullBuilder;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.skull.config.SkullConfig;
import net.exylia.commons.v2.skull.core.SkullManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SkullAPI {

    public static void initialize(Plugin plugin) {
        SkullConfig config = new SkullConfig();
        config.setDataFolder(plugin.getDataFolder());
        SkullManager.initialize(config);
    }

    public static void initialize(Plugin plugin, SkullConfig config) {
        config.setDataFolder(plugin.getDataFolder());
        SkullManager.initialize(config);
    }

    public static void shutdown() {
        if (SkullManager.isInitialized()) {
            SkullManager.getInstance().shutdown();
        }
    }

    public static boolean isInitialized() {
        return SkullManager.isInitialized();
    }

    private static SkullManager getManager() {
        return SkullManager.getInstance();
    }

    public static ItemStack fromTexture(String base64) {
        return getManager().getRenderer().renderTexture(base64);
    }

    public static CompletableFuture<ItemStack> fromTextureAsync(String base64) {
        return getManager().getRenderer().renderTextureAsync(base64);
    }

    public static void fromTextureAsync(String base64, Consumer<ItemStack> consumer) {
        getManager().getRenderer().renderTextureAsync(base64)
                .thenAccept(skull -> Tasks.sync(() -> consumer.accept(skull)));
    }

    public static ItemStack fromTextureURL(String url) {
        return getManager().getRenderer().renderTextureURL(url);
    }

    public static CompletableFuture<ItemStack> fromTextureURLAsync(String url) {
        return getManager().getRenderer().renderTextureURLAsync(url);
    }

    public static void fromTextureURLAsync(String url, Consumer<ItemStack> consumer) {
        getManager().getRenderer().renderTextureURLAsync(url)
                .thenAccept(skull -> Tasks.sync(() -> consumer.accept(skull)));
    }

    public static ItemStack fromPlayer(String playerName) {
        return getManager().getRenderer().renderPlayer(playerName);
    }

    public static ItemStack fromPlayerCached(String playerName) {
        return getManager().getRenderer().renderPlayer(playerName);
    }

    public static CompletableFuture<ItemStack> fromPlayerAsync(String playerName) {
        return getManager().getRenderer().renderPlayerAsync(playerName);
    }

    public static void fromPlayerAsync(String playerName, Consumer<ItemStack> consumer) {
        getManager().getRenderer().renderPlayerAsync(playerName)
                .thenAccept(skull -> Tasks.sync(() -> consumer.accept(skull)));
    }

    public static SkullBuilder texture(String base64) {
        return SkullBuilder.texture(getManager().getRenderer(), base64);
    }

    public static SkullBuilder textureURL(String url) {
        return SkullBuilder.textureURL(getManager().getRenderer(), url);
    }

    public static SkullBuilder player(String playerName) {
        return SkullBuilder.player(getManager().getRenderer(), playerName);
    }

    public static void preloadPlayers(String... playerNames) {
        getManager().preloadPlayers(playerNames);
    }

    public static CompletableFuture<List<ItemStack>> batchPlayers(String... playerNames) {
        return getManager().batchPlayers(playerNames);
    }

    public static boolean isPlayerCached(String playerName) {
        return getManager().isPlayerCached(playerName);
    }

    public static boolean isRateLimited() {
        return getManager().getCache().isRateLimited();
    }

    public static void invalidatePlayer(String playerName) {
        getManager().getCache().clearPlayer(playerName);
        if (getManager().getPersistence() != null) {
            getManager().getPersistence().invalidate(playerName);
        }
    }

    public static void clearTextureCache() {
        getManager().getCache().clearTextures();
    }

    public static void clearPlayerCache() {
        getManager().getCache().clearPlayers();
    }

    public static void clearAllCache() {
        getManager().getCache().clearAll();
        if (getManager().getPersistence() != null) {
            getManager().getPersistence().clear();
        }
    }

    public static int getPersistentCacheSize() {
        if (getManager().getPersistence() == null) {
            return 0;
        }
        return getManager().getPersistence().size();
    }

    public static String getStats() {
        return getManager().getStats();
    }
}
