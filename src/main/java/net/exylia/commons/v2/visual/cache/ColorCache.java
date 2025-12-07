package net.exylia.commons.v2.visual.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.v2.visual.color.ColorProcessor;

import java.util.concurrent.TimeUnit;

public class ColorCache {
    private static final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .build();

    public static String getOrParse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return cache.get(text, ColorProcessor::processString);
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
