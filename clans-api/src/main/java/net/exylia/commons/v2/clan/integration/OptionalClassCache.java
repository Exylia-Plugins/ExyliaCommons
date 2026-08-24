package net.exylia.commons.v2.clan.integration;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache for optional class resolution.
 *
 * <p>Wraps {@link Class#forName(String)} so that both positive and negative results
 * are memoized for the lifetime of the JVM. This avoids repeatedly walking every
 * installed plugin's classloader on cache misses — a known source of MSPT spikes
 * when the target class belongs to an optional, not-installed dependency.
 *
 * <p>Usage:
 * <pre>{@code
 * private static Class<?> resolveClanApi() {
 *     return OptionalClassCache.resolve("net.exylia.commons.v2.clan.api.ClanAPI");
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class OptionalClassCache {

    private static final ConcurrentMap<String, Optional<Class<?>>> CACHE = new ConcurrentHashMap<>();

    private OptionalClassCache() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves a class by name, caching both success and failure.
     *
     * @param className the fully-qualified class name
     * @return the {@link Class} object, or {@code null} if not found
     */
    public static Class<?> resolve(String className) {
        return CACHE.computeIfAbsent(className, name -> {
            try {
                return Optional.of(Class.forName(name));
            } catch (ClassNotFoundException | LinkageError e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * Returns the number of cached entries.
     */
    public static int size() {
        return CACHE.size();
    }

    /**
     * Clears the cache. Useful for plugin reloads or testing.
     */
    public static void clear() {
        CACHE.clear();
    }
}
