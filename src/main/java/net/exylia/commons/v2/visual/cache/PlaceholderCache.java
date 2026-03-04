package net.exylia.commons.v2.visual.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PlaceholderCache {
    private static final Cache<CacheKey, String> papiCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .executor(Runnable::run)
            .build();

    public static String getOrProcess(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Player targetPlayer = context != null && context.getPlayer() != null
                ? context.getPlayer()
                : player;

        String contextProcessed = Placeholders.processContextOnly(text, targetPlayer, context);

        if (targetPlayer == null || !contextProcessed.contains("%")) {
            return contextProcessed;
        }

        CacheKey key = CacheKey.of(contextProcessed, targetPlayer.getUniqueId(), 0);
        return papiCache.get(key, k -> Placeholders.processPapiOnly(contextProcessed, targetPlayer));
    }

    public static void invalidate(Player player) {
        papiCache.asMap().keySet().removeIf(key -> key.getPlayerId() != null && key.getPlayerId().equals(player.getUniqueId()));
    }

    public static void clear() {
        papiCache.invalidateAll();
    }

    public static long size() {
        return papiCache.estimatedSize();
    }

    public static com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return papiCache.stats();
    }
}
