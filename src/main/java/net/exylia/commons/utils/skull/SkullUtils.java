package net.exylia.commons.utils.skull;

import org.bukkit.inventory.ItemStack;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for easy skull creation - wrapper around SkullManager
 * Uses LiteSkullAPI only for player skulls, original implementation for textures
 */
public class SkullUtils {

    /**
     * Create a skull from base64 texture data (uses original implementation)
     * @param base64 The base64 encoded texture data
     * @return ItemStack with the skull
     */
    public static ItemStack createSkullFromTexture(String base64) {
        return SkullManager.getInstance().createSkullFromTexture(base64);
    }

    /**
     * Create a skull from a texture URL (uses original implementation)
     * @param url The texture URL (can be full URL or just the texture hash)
     * @return ItemStack with the skull
     */
    public static ItemStack createSkullFromUrl(String url) {
        return SkullManager.getInstance().createSkullFromUrl(url);
    }

    /**
     * Create a player skull synchronously (uses LiteSkullAPI if available)
     * Returns basic skull immediately, actual textured skull cached by LiteSkullAPI
     * @param playerName The player name
     * @return ItemStack with the skull
     */
    public static ItemStack createPlayerSkull(String playerName) {
        return SkullManager.getInstance().createPlayerSkull(playerName);
    }

    /**
     * Create a player skull asynchronously (uses LiteSkullAPI if available)
     * @param playerName The player name
     * @return CompletableFuture with the skull
     */
    public static CompletableFuture<ItemStack> createPlayerSkullAsync(String playerName) {
        return SkullManager.getInstance().createPlayerSkullAsync(playerName);
    }

    /**
     * Accept a player skull synchronously using LiteSkullAPI (if available)
     * Runs in server sync task when skull is ready
     * @param playerName The player name
     * @param consumer Consumer to handle the skull when ready
     */
    public static void acceptSyncPlayerSkull(String playerName, java.util.function.Consumer<ItemStack> consumer) {
        SkullManager.getInstance().acceptSyncPlayerSkull(playerName, consumer);
    }

    /**
     * Accept a player skull asynchronously using LiteSkullAPI (if available)
     * @param playerName The player name
     * @param consumer Consumer to handle the skull when ready
     */
    public static void acceptAsyncPlayerSkull(String playerName, java.util.function.Consumer<ItemStack> consumer) {
        SkullManager.getInstance().acceptAsyncPlayerSkull(playerName, consumer);
    }

    /**
     * Encode a texture URL to base64
     * @param url The texture URL
     * @return Base64 encoded texture data
     */
    public static String encodeTextureUrl(String url) {
        String fullUrl = url.startsWith("http") ? url : "http://textures.minecraft.net/texture/" + url;
        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", fullUrl);
        return Base64.getEncoder().encodeToString(textureJson.getBytes());
    }

    /**
     * Preload player skulls for better performance (uses LiteSkullAPI if available)
     * @param playerNames Array of player names to preload
     */
    public static void preloadPlayerSkulls(String... playerNames) {
        SkullManager.getInstance().preloadPlayerSkulls(playerNames);
    }

    /**
     * Get cache statistics
     * @return String with cache information
     */
    public static String getCacheStats() {
        return SkullManager.getInstance().getCacheStats();
    }

    /**
     * Clear all caches
     */
    public static void clearCache() {
        SkullManager.getInstance().clearCache();
    }

    /**
     * Clear only player cache (managed by LiteSkullAPI)
     */
    public static void clearPlayerCache() {
        SkullManager.getInstance().clearPlayerCache();
    }

    /**
     * Clear only texture cache
     */
    public static void clearTextureCache() {
        SkullManager.getInstance().clearTextureCache();
    }

    // Common skull textures for quick access (uses original implementation)
    public static class CommonSkulls {

        // Arrow skulls
        public static final String ARROW_LEFT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
        public static final String ARROW_RIGHT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
        public static final String ARROW_UP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQxMzNmNmFjM2JlMmUyNDk5YTc4NGVmYWRjZmZmZWI5YWNlMDI1YzM2NDZhZGE2N2YzNDE0ZTVlZjMzOTQifX19";
        public static final String ARROW_DOWN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzI0MzE5MTFmNDE3OGI0ZDJiNDEzYWE3ZjVjNzhhZTQ0NDdmZTkyNDY5NDNjMzFkZjMxMTc2NzY5YzNiMmNhMiJ9fX0=";

        // Utility skulls
        public static final String PLUS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMzE0MzFkNjQ1ODdmZjZlZjk4YzA2NzU4MTA2ODFmOGMxM2JmOTZmNTFkOWNiMDdlZDc4NTJiMmZmZDEifX19";
        public static final String MINUS = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU0YjhiOGQyMzYyYzg2NGFhNGRjNzEyNGNjNzU3ZjkxZDQ1YjlkMjIxNDU3YzIwMTk3ZjhhZGY0MTJlNmNhYiJ9fX0=";
        public static final String CHECKMARK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDMxMmNhNDYzMmRlZjVmZmFmMmViMGQ5ZDdjYzdiNTVhNTBjNGUzOTIwNTQ5ZTVmZmQxMzFkNzgyOTc5NzAifX19";
        public static final String X_MARK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==";

        // Color skulls
        public static final String RED = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZkZTNiZmNlMmQ4Y2I3MjRkZTk5NzlkNDhiNDdlZjgxNGQ3N2MyOGU3NzZjNzg5YzJkM2ZmNGQ2NjczYjk5YiJ9fX0=";
        public static final String GREEN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYxNzBkNzUxMzJkNzRkZDQzMjJkNGNlZjM0MzZkNGNhMzJiNzk5YzY2NDlkNDk3YzUzNjE2NzYxNDIzNzMwNSJ9fX0=";
        public static final String BLUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGQ2OGRlNzEzNzFiNGM3Nzc3OTZmNzYxNzU2MzJjNzU4YjE3NzI3ZjNkMjU2YjVhNzU2MWM5YjQxNTc5NDY1In19fQ==";
        public static final String YELLOW = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FiMDk2YjU3YzI5YzNkNzNkMjYzNzc4NmNmZGRkZDZkNDc2YWFkNzlkNGQ3YzQ1NDhjNzVkNGNlMzc5ZjE0YSJ9fX0=";

        /**
         * Get arrow left skull
         */
        public static ItemStack getArrowLeft() {
            return createSkullFromTexture(ARROW_LEFT);
        }

        /**
         * Get arrow right skull
         */
        public static ItemStack getArrowRight() {
            return createSkullFromTexture(ARROW_RIGHT);
        }

        /**
         * Get arrow up skull
         */
        public static ItemStack getArrowUp() {
            return createSkullFromTexture(ARROW_UP);
        }

        /**
         * Get arrow down skull
         */
        public static ItemStack getArrowDown() {
            return createSkullFromTexture(ARROW_DOWN);
        }

        /**
         * Get plus skull
         */
        public static ItemStack getPlus() {
            return createSkullFromTexture(PLUS);
        }

        /**
         * Get minus skull
         */
        public static ItemStack getMinus() {
            return createSkullFromTexture(MINUS);
        }

        /**
         * Get checkmark skull
         */
        public static ItemStack getCheckmark() {
            return createSkullFromTexture(CHECKMARK);
        }

        /**
         * Get X mark skull
         */
        public static ItemStack getXMark() {
            return createSkullFromTexture(X_MARK);
        }

        /**
         * Get red skull
         */
        public static ItemStack getRed() {
            return createSkullFromTexture(RED);
        }

        /**
         * Get green skull
         */
        public static ItemStack getGreen() {
            return createSkullFromTexture(GREEN);
        }

        /**
         * Get blue skull
         */
        public static ItemStack getBlue() {
            return createSkullFromTexture(BLUE);
        }

        /**
         * Get yellow skull
         */
        public static ItemStack getYellow() {
            return createSkullFromTexture(YELLOW);
        }
    }
}