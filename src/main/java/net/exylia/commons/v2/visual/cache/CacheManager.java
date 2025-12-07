package net.exylia.commons.v2.visual.cache;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Getter
public class CacheManager {
    private static volatile CacheManager instance;
    private static final Object LOCK = new Object();

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CacheManager();
                }
            }
        }
        return instance;
    }

    public Component processAndParse(String text, Player player, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String withPlaceholders = PlaceholderCache.getOrProcess(text, player, context);

        String withColors = ColorCache.getOrParse(withPlaceholders);

        return ComponentCache.getOrParse(withColors);
    }

    public String processPlaceholders(String text, Player player, PlaceholderContext context) {
        return PlaceholderCache.getOrProcess(text, player, context);
    }

    public String parseColors(String text) {
        return ColorCache.getOrParse(text);
    }

    public Component parseComponent(String text) {
        return ComponentCache.getOrParse(text);
    }

    public void invalidatePlayer(Player player) {
        PlaceholderCache.invalidate(player);
    }

    public void clearAll() {
        PlaceholderCache.clear();
        ColorCache.clear();
        ComponentCache.clear();
    }

    public CacheStats getStats() {
        return new CacheStats(
                PlaceholderCache.size(),
                ColorCache.size(),
                ComponentCache.size(),
                PlaceholderCache.stats(),
                ColorCache.stats(),
                ComponentCache.stats()
        );
    }

    @Getter
    public static class CacheStats {
        private final long placeholderCacheSize;
        private final long colorCacheSize;
        private final long componentCacheSize;
        private final com.github.benmanes.caffeine.cache.stats.CacheStats placeholderStats;
        private final com.github.benmanes.caffeine.cache.stats.CacheStats colorStats;
        private final com.github.benmanes.caffeine.cache.stats.CacheStats componentStats;

        public CacheStats(
                long placeholderCacheSize,
                long colorCacheSize,
                long componentCacheSize,
                com.github.benmanes.caffeine.cache.stats.CacheStats placeholderStats,
                com.github.benmanes.caffeine.cache.stats.CacheStats colorStats,
                com.github.benmanes.caffeine.cache.stats.CacheStats componentStats
        ) {
            this.placeholderCacheSize = placeholderCacheSize;
            this.colorCacheSize = colorCacheSize;
            this.componentCacheSize = componentCacheSize;
            this.placeholderStats = placeholderStats;
            this.colorStats = colorStats;
            this.componentStats = componentStats;
        }

        public long getTotalSize() {
            return placeholderCacheSize + colorCacheSize + componentCacheSize;
        }
    }
}
