package net.exylia.commons.v2.placeholders.registry;

import net.exylia.commons.v2.placeholders.annotation.Placeholder;
import net.exylia.commons.v2.placeholders.annotation.PlaceholderScope;
import net.exylia.commons.v2.placeholders.async.AsyncPlaceholderExecutor;
import net.exylia.commons.v2.placeholders.cache.PlaceholderCacheV2;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.exception.PlaceholderRegistrationException;
import net.exylia.commons.v2.placeholders.resolver.ContextPlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.GlobalPlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.PlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.PlayerPlaceholderResolver;
import net.exylia.commons.v2.placeholders.scanner.PlaceholderAnnotationScanner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class PlaceholderRegistryV2 {
    private static PlaceholderRegistryV2 instance;

    private final JavaPlugin plugin;
    private final PlaceholderCacheV2 cache;
    private final AsyncPlaceholderExecutor asyncExecutor;

    private final Map<String, PlaceholderResolver> resolvers = new ConcurrentHashMap<>();
    private final Map<String, GlobalPlaceholderResolver> globalResolvers = new ConcurrentHashMap<>();
    private final Map<String, PlayerPlaceholderResolver> playerResolvers = new ConcurrentHashMap<>();
    private final Map<String, ContextPlaceholderResolver> contextResolvers = new ConcurrentHashMap<>();

    private final PlaceholderAnnotationScanner scanner;
    private boolean initialized = false;

    private PlaceholderRegistryV2(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cache = new PlaceholderCacheV2(1000);
        this.asyncExecutor = AsyncPlaceholderExecutor.getInstance();
        this.scanner = new PlaceholderAnnotationScanner();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (PlaceholderRegistryV2.class) {
                if (instance == null) {
                    instance = new PlaceholderRegistryV2(plugin);
                }
            }
        }
    }

    public static PlaceholderRegistryV2 getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlaceholderRegistryV2 not initialized. Call initialize() first.");
        }
        return instance;
    }

    public void registerAnnotatedClass(Object instance) throws PlaceholderRegistrationException {
        List<Method> methods = scanner.scanClass(instance.getClass());
        for (Method method : methods) {
            Placeholder annotation = method.getAnnotation(Placeholder.class);
            if (annotation != null) {
                registerMethod(instance, method, annotation);
            }
        }
    }

    public void registerAnnotatedClasses(Object... instances) throws PlaceholderRegistrationException {
        for (Object instance : instances) {
            registerAnnotatedClass(instance);
        }
    }

    private void registerMethod(Object instance, Method method, Placeholder annotation) throws PlaceholderRegistrationException {
        String name = annotation.name().toLowerCase();
        PlaceholderScope scope = annotation.scope();

        PlaceholderResolver resolver = new PlaceholderResolver(name, method, instance, annotation);
        resolvers.put(name, resolver);

        switch (scope) {
            case GLOBAL:
                globalResolvers.put(name, () -> resolver.resolve(null, null));
                break;
            case PLAYER:
                playerResolvers.put(name, player -> resolver.resolve(player, null));
                break;
            case CONTEXT:
                contextResolvers.put(name, (context, player) -> resolver.resolve(player, context));
                break;
        }

        cache.invalidatePattern(name);
    }

    public void registerGlobal(String name, GlobalPlaceholderResolver resolver) {
        String key = name.toLowerCase();
        globalResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public void registerPlayer(String name, PlayerPlaceholderResolver resolver) {
        String key = name.toLowerCase();
        playerResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public void registerContext(String name, ContextPlaceholderResolver resolver) {
        String key = name.toLowerCase();
        contextResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public Object resolve(String name, Player player, PlaceholderContext context) {
        String key = name.toLowerCase();

        if (context != null && context.has(key)) {
            return context.get(key);
        }

        if (context != null) {
            ContextPlaceholderResolver contextResolver = contextResolvers.get(key);
            if (contextResolver != null) {
                return safeResolve(() -> contextResolver.resolve(context, player));
            }
        }

        if (player != null) {
            PlayerPlaceholderResolver playerResolver = playerResolvers.get(key);
            if (playerResolver != null) {
                return safeResolve(() -> playerResolver.resolve(player));
            }
        }

        GlobalPlaceholderResolver globalResolver = globalResolvers.get(key);
        if (globalResolver != null) {
            return safeResolve(globalResolver::resolve);
        }

        return null;
    }

    public CompletableFuture<String> resolveAsync(String name, Player player, PlaceholderContext context) {
        String key = name.toLowerCase();

        if (context != null && context.has(key)) {
            Object value = context.get(key);
            return CompletableFuture.completedFuture(value != null ? value.toString() : null);
        }

        if (context != null) {
            ContextPlaceholderResolver contextResolver = contextResolvers.get(key);
            if (contextResolver != null) {
                return asyncExecutor.executeAsyncPlaceholder(() -> contextResolver.resolve(context, player));
            }
        }

        if (player != null) {
            PlayerPlaceholderResolver playerResolver = playerResolvers.get(key);
            if (playerResolver != null) {
                return asyncExecutor.executeAsyncPlaceholder(() -> playerResolver.resolve(player));
            }
        }

        GlobalPlaceholderResolver globalResolver = globalResolvers.get(key);
        if (globalResolver != null) {
            return asyncExecutor.executeAsyncPlaceholder(globalResolver::resolve);
        }

        return CompletableFuture.completedFuture(null);
    }

    private Object safeResolve(PlaceholderSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasResolver(String name) {
        String key = name.toLowerCase();
        return globalResolvers.containsKey(key) ||
                playerResolvers.containsKey(key) ||
                contextResolvers.containsKey(key);
    }

    public PlaceholderResolver getResolver(String name) {
        return resolvers.get(name.toLowerCase());
    }

    public PlaceholderCacheV2 getCache() {
        return cache;
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    public void shutdown() {
        cache.invalidateAll();
        resolvers.clear();
        globalResolvers.clear();
        playerResolvers.clear();
        contextResolvers.clear();
    }

    public PlaceholderRegistryStats getStats() {
        return new PlaceholderRegistryStats(
                globalResolvers.size(),
                playerResolvers.size(),
                contextResolvers.size(),
                cache.getStats()
        );
    }

    public Set<String> getRegisteredPlaceholders() {
        Set<String> all = new HashSet<>();
        all.addAll(globalResolvers.keySet());
        all.addAll(playerResolvers.keySet());
        all.addAll(contextResolvers.keySet());
        return all;
    }

    @FunctionalInterface
    private interface PlaceholderSupplier {
        Object get() throws Exception;
    }

    public record PlaceholderRegistryStats(
            int globalCount,
            int playerCount,
            int contextCount,
            PlaceholderCacheV2.PlaceholderCacheStats cacheStats
    ) {
        public int getTotalCount() {
            return globalCount + playerCount + contextCount;
        }

        @Override
        public String toString() {
            return String.format("PlaceholderRegistry{global=%d, player=%d, context=%d, total=%d, cache=%s}",
                    globalCount, playerCount, contextCount, getTotalCount(), cacheStats);
        }
    }
}
