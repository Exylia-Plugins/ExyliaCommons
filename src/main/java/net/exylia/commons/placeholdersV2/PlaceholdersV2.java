package net.exylia.commons.placeholdersV2;

import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import net.exylia.commons.placeholdersV2.papi.PapiAdapterV2;
import net.exylia.commons.placeholdersV2.processor.PlaceholderProcessorV2;
import net.exylia.commons.placeholdersV2.registry.PlaceholderRegistryV2;
import net.exylia.commons.placeholdersV2.resolver.ContextPlaceholderResolver;
import net.exylia.commons.placeholdersV2.resolver.GlobalPlaceholderResolver;
import net.exylia.commons.placeholdersV2.resolver.PlayerPlaceholderResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PlaceholdersV2 {
    private PlaceholdersV2() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        PlaceholderRegistryV2.initialize(plugin);
        PapiAdapterV2.initialize(plugin);
    }

    public static void registerPapiExpander(String identifier) {
        PapiAdapterV2.getInstance().registerExpander(identifier);
    }

    public static void registerAnnotatedClass(Object instance) {
        PlaceholderRegistryV2.getInstance().registerAnnotatedClass(instance);
    }

    public static void registerAnnotatedClasses(Object... instances) {
        PlaceholderRegistryV2.getInstance().registerAnnotatedClasses(instances);
    }

    public static void registerGlobal(String name, GlobalPlaceholderResolver resolver) {
        PlaceholderRegistryV2.getInstance().registerGlobal(name, resolver);
    }

    public static void registerPlayer(String name, PlayerPlaceholderResolver resolver) {
        PlaceholderRegistryV2.getInstance().registerPlayer(name, resolver);
    }

    public static void registerContext(String name, ContextPlaceholderResolver resolver) {
        PlaceholderRegistryV2.getInstance().registerContext(name, resolver);
    }

    public static String process(String text, Player player, PlaceholderContext context) {
        return PlaceholderProcessorV2.process(text, player, context);
    }

    public static String process(String text, Player player) {
        return PlaceholderProcessorV2.process(text, player);
    }

    public static String process(String text, PlaceholderContext context) {
        return PlaceholderProcessorV2.process(text, context);
    }

    public static String process(String text) {
        return PlaceholderProcessorV2.process(text);
    }

    public static CompletableFuture<String> processAsync(String text, Player player, PlaceholderContext context) {
        return PlaceholderProcessorV2.processAsync(text, player, context);
    }

    public static CompletableFuture<String> processAsync(String text, Player player) {
        return PlaceholderProcessorV2.processAsync(text, player);
    }

    public static CompletableFuture<String> processAsync(String text, PlaceholderContext context) {
        return PlaceholderProcessorV2.processAsync(text, context);
    }

    public static CompletableFuture<String> processAsync(String text) {
        return PlaceholderProcessorV2.processAsync(text);
    }

    public static List<String> extractPlaceholders(String text) {
        return PlaceholderProcessorV2.extractPlaceholders(text);
    }

    public static boolean containsPlaceholders(String text) {
        return PlaceholderProcessorV2.containsPlaceholders(text);
    }

    public static boolean hasResolver(String name) {
        return PlaceholderRegistryV2.getInstance().hasResolver(name);
    }

    public static Set<String> getRegisteredPlaceholders() {
        return PlaceholderRegistryV2.getInstance().getRegisteredPlaceholders();
    }

    public static void clearCache() {
        PlaceholderRegistryV2.getInstance().clearCache();
    }

    public static void shutdown() {
        PapiAdapterV2.getInstance().unregister();
        PlaceholderRegistryV2.getInstance().shutdown();
    }

    public static PlaceholderRegistryV2.PlaceholderRegistryStats getStats() {
        return PlaceholderRegistryV2.getInstance().getStats();
    }

    public static PlaceholderContext createContext() {
        return PlaceholderContext.create();
    }

    public static PlaceholderContext createContext(Player player) {
        return PlaceholderContext.create().withPlayer(player);
    }
}
