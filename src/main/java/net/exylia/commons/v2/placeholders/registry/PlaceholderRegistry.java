package net.exylia.commons.v2.placeholders.registry;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.annotation.Placeholder;
import net.exylia.commons.v2.placeholders.annotation.PlaceholderScope;
import net.exylia.commons.v2.placeholders.async.AsyncPlaceholderExecutor;
import net.exylia.commons.v2.placeholders.cache.PlaceholderCache;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.exception.PlaceholderRegistrationException;
import net.exylia.commons.v2.placeholders.papi.PapiAdapter;
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

public class PlaceholderRegistry {
    private static PlaceholderRegistry instance;

    private final JavaPlugin plugin;
    private final PlaceholderCache cache;
    private final AsyncPlaceholderExecutor asyncExecutor;

    private final Map<String, PlaceholderResolver> resolvers = new ConcurrentHashMap<>();
    private final Map<String, GlobalPlaceholderResolver> globalResolvers = new ConcurrentHashMap<>();
    private final Map<String, PlayerPlaceholderResolver> playerResolvers = new ConcurrentHashMap<>();
    private final Map<String, ContextPlaceholderResolver> contextResolvers = new ConcurrentHashMap<>();
    private final Map<String, PlaceholderResolver> argumentResolvers = new ConcurrentHashMap<>();

    private final PlaceholderAnnotationScanner scanner;
    private boolean initialized = false;

    private PlaceholderRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cache = new PlaceholderCache(1000);
        this.asyncExecutor = AsyncPlaceholderExecutor.getInstance();
        this.scanner = new PlaceholderAnnotationScanner();
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (PlaceholderRegistry.class) {
            if (instance != null && instance.plugin == plugin) {
                return;
            }

            if (instance != null) {
                instance.shutdown();
            }

            instance = new PlaceholderRegistry(plugin);
        }
    }

    public static PlaceholderRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlaceholderRegistryV2 not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public void registerAnnotatedClass(Object instance) throws PlaceholderRegistrationException {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Scanning class for placeholders: " + instance.getClass().getSimpleName());
        List<Method> methods = scanner.scanClass(instance.getClass());
        int registered = 0;
        for (Method method : methods) {
            Placeholder annotation = method.getAnnotation(Placeholder.class);
            if (annotation != null) {
                registerMethod(instance, method, annotation);
                registered++;
            }
        }
        DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER,
            String.format("Registered %d placeholder(s) from class: %s", registered, instance.getClass().getSimpleName()));
    }

    public void registerAnnotatedClasses(Object... instances) throws PlaceholderRegistrationException {
        for (Object instance : instances) {
            registerAnnotatedClass(instance);
        }
    }

    private void registerMethod(Object instance, Method method, Placeholder annotation) throws PlaceholderRegistrationException {
        String name = annotation.name().toLowerCase();
        PlaceholderScope scope = annotation.scope();

        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
            String.format("Registering placeholder '%s' with scope: %s, hasArgument: %s", name, scope, annotation.hasArgument() || name.endsWith("_*")));

        PlaceholderResolver resolver = new PlaceholderResolver(name, method, instance, annotation);
        resolvers.put(name, resolver);

        if (resolver.hasArgument()) {
            argumentResolvers.put(name, resolver);
        } else {
            argumentResolvers.remove(name);
        }

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
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Registering global placeholder: " + key);
        globalResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public void registerPlayer(String name, PlayerPlaceholderResolver resolver) {
        String key = name.toLowerCase();
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Registering player placeholder: " + key);
        playerResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public void registerContext(String name, ContextPlaceholderResolver resolver) {
        String key = name.toLowerCase();
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Registering context placeholder: " + key);
        contextResolvers.put(key, resolver);
        cache.invalidatePattern(key);
    }

    public Object resolve(String name, Player player, PlaceholderContext context) {
        String key = name.toLowerCase();
        long startTime = System.nanoTime();

        if (context != null && context.has(key)) {
            Object value = safeResolve(() -> context.get(key), key);
            logResolveSuccess(key, "context", System.nanoTime() - startTime);
            return value;
        }

        if (context != null) {
            ContextPlaceholderResolver contextResolver = contextResolvers.get(key);
            if (contextResolver != null) {
                Object result = safeResolve(() -> contextResolver.resolve(context, player), key);
                logResolveSuccess(key, "context-resolver", System.nanoTime() - startTime);
                return result;
            }
        }

        if (player != null) {
            PlayerPlaceholderResolver playerResolver = playerResolvers.get(key);
            if (playerResolver != null) {
                Object result = safeResolve(() -> playerResolver.resolve(player), key);
                logResolveSuccess(key, "player-resolver", System.nanoTime() - startTime);
                return result;
            }
        }

        GlobalPlaceholderResolver globalResolver = globalResolvers.get(key);
        if (globalResolver != null) {
            Object result = safeResolve(globalResolver::resolve, key);
            logResolveSuccess(key, "global-resolver", System.nanoTime() - startTime);
            return result;
        }

        for (PlaceholderResolver resolver : argumentResolvers.values()) {
            if (resolver.matches(key)) {
                String argument = resolver.extractArgument(name);
                Object result = safeResolve(() -> resolver.resolve(player, context, argument), key);
                logResolveSuccess(key, "argument-resolver(" + resolver.getName() + ")", System.nanoTime() - startTime);
                return result;
            }
        }

        try {
            PapiAdapter papiAdapter = PapiAdapter.getInstance();
            if (papiAdapter != null && !papiAdapter.canResolvePlaceholder(key)) {
                DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "No resolver found for placeholder: " + key);
            }
        } catch (Exception e) {
            DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "No resolver found for placeholder: " + key);
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

        for (PlaceholderResolver resolver : argumentResolvers.values()) {
            if (resolver.matches(key)) {
                String argument = resolver.extractArgument(name);
                return asyncExecutor.executeAsyncPlaceholder(() -> resolver.resolve(player, context, argument));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private Object safeResolve(PlaceholderSupplier supplier, String placeholderName) {
        try {
            return supplier.get();
        } catch (Exception e) {
            DebugAPI.logLibError(DebugCategory.PLACEHOLDER,
                String.format("Error resolving placeholder '%s': %s", placeholderName, e.getMessage()), e);
            return null;
        }
    }

    private void logResolveSuccess(String name, String resolverType, long nanos) {
//        double millis = nanos / 1_000_000.0;d
//        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER,
//            String.format("Resolved '%s' via %s in %.3fms", name, resolverType, millis));
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

    public PlaceholderCache getCache() {
        return cache;
    }

    public void clearCache() {
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Clearing placeholder cache");
        cache.invalidateAll();
    }

    public void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.PLACEHOLDER, "Shutting down PlaceholderRegistry");
        cache.invalidateAll();
        resolvers.clear();
        globalResolvers.clear();
        playerResolvers.clear();
        contextResolvers.clear();
        argumentResolvers.clear();
        initialized = false;
        synchronized (PlaceholderRegistry.class) {
            if (instance == this) {
                instance = null;
            }
        }
        DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "PlaceholderRegistry shutdown complete");
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
            PlaceholderCache.PlaceholderCacheStats cacheStats
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
