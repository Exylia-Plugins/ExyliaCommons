package net.exylia.commons.v2.placeholders.api;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.papi.PapiAdapter;
import net.exylia.commons.v2.placeholders.processor.PlaceholderProcessor;
import net.exylia.commons.v2.placeholders.registry.PlaceholderRegistry;
import net.exylia.commons.v2.placeholders.resolver.ContextPlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.GlobalPlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.PlayerPlaceholderResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Placeholders {
    private Placeholders() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        DebugAPI.logLibInfo(DebugCategory.PLACEHOLDER, "Initializing Placeholder System");
        PlaceholderRegistry.initialize(plugin);
        PapiAdapter.initialize(plugin);
        DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "Placeholder System initialized successfully");
    }

    public static void registerPapiExpander(String identifier) {
        PapiAdapter.getInstance().registerExpander(identifier);
    }

    public static void registerAnnotatedClass(Object instance) {
        PlaceholderRegistry.getInstance().registerAnnotatedClass(instance);
    }

    public static void registerAnnotatedClasses(Object... instances) {
        PlaceholderRegistry.getInstance().registerAnnotatedClasses(instances);
    }

    public static void registerGlobal(String name, GlobalPlaceholderResolver resolver) {
        PlaceholderRegistry.getInstance().registerGlobal(name, resolver);
    }

    public static void registerPlayer(String name, PlayerPlaceholderResolver resolver) {
        PlaceholderRegistry.getInstance().registerPlayer(name, resolver);
    }

    public static void registerContext(String name, ContextPlaceholderResolver resolver) {
        PlaceholderRegistry.getInstance().registerContext(name, resolver);
    }

    public static String process(String text, Player player, PlaceholderContext context) {
        return PlaceholderProcessor.process(text, player, context);
    }

    public static String process(String text, Player player) {
        return PlaceholderProcessor.process(text, player);
    }

    public static String process(String text, PlaceholderContext context) {
        return PlaceholderProcessor.process(text, context);
    }

    public static String process(String text) {
        return PlaceholderProcessor.process(text);
    }

    public static CompletableFuture<String> processAsync(String text, Player player, PlaceholderContext context) {
        return PlaceholderProcessor.processAsync(text, player, context);
    }

    public static CompletableFuture<String> processAsync(String text, Player player) {
        return PlaceholderProcessor.processAsync(text, player);
    }

    public static CompletableFuture<String> processAsync(String text, PlaceholderContext context) {
        return PlaceholderProcessor.processAsync(text, context);
    }

    public static CompletableFuture<String> processAsync(String text) {
        return PlaceholderProcessor.processAsync(text);
    }

    public static List<String> extractPlaceholders(String text) {
        return PlaceholderProcessor.extractPlaceholders(text);
    }

    public static boolean containsPlaceholders(String text) {
        return PlaceholderProcessor.containsPlaceholders(text);
    }

    public static boolean hasResolver(String name) {
        return PlaceholderRegistry.getInstance().hasResolver(name);
    }

    public static Set<String> getRegisteredPlaceholders() {
        return PlaceholderRegistry.getInstance().getRegisteredPlaceholders();
    }

    public static void clearCache() {
        PlaceholderRegistry.getInstance().clearCache();
    }

    public static void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.PLACEHOLDER, "Shutting down Placeholder System");
        PapiAdapter.shutdown();
        if (PlaceholderRegistry.isInitialized()) {
            PlaceholderRegistry.getInstance().shutdown();
        }
        DebugAPI.logLibSuccess(DebugCategory.PLACEHOLDER, "Placeholder System shutdown complete");
    }

    public static PlaceholderRegistry.PlaceholderRegistryStats getStats() {
        PlaceholderRegistry.PlaceholderRegistryStats stats = PlaceholderRegistry.getInstance().getStats();
        DebugAPI.logLibDebug(DebugCategory.PLACEHOLDER, "Stats: " + stats.toString());
        return stats;
    }

    public static PlaceholderContext createContext() {
        return PlaceholderContext.create();
    }

    public static PlaceholderContext createContext(Player player) {
        return PlaceholderContext.create().withPlayer(player);
    }
}
