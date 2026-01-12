package net.exylia.commons.v2.visual.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PlaceholderCache {
    private static final Cache<CacheKey, String> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .recordStats()
            .build();

    public static String getOrProcess(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Player targetPlayer = context != null && context.getPlayer() != null
                ? context.getPlayer()
                : player;

        if (targetPlayer == null) {
            return Placeholders.process(text, null, context);
        }

        CacheKey key = CacheKey.of(text, targetPlayer.getUniqueId(), context.hashCode());

        return cache.get(key, k -> Placeholders.process(text, targetPlayer, context));
    }

    public static void invalidate(Player player) {
        cache.asMap().keySet().removeIf(key -> key.getPlayerId() != null && key.getPlayerId().equals(player.getUniqueId()));
    }

    public static void clear() {
        cache.invalidateAll();
    }

    public static long size() {
        return cache.estimatedSize();
    }

    public static com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }
}
