package net.exylia.commons.v2.visual.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.visual.color.ColorProcessor;
import net.kyori.adventure.text.Component;


public class ComponentCache {
    private static final Cache<String, Component> cache = Caffeine.newBuilder()
            .maximumSize(2000)
            .recordStats()
            .build();

    public static Component getOrParse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        return cache.get(text, ColorProcessor::parseToComponent);
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
