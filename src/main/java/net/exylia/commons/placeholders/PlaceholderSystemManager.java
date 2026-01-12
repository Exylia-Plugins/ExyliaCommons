package net.exylia.commons.placeholders;

import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.placeholders.resolver.GlobalPlaceholderResolver;
import net.exylia.commons.v2.placeholders.resolver.PlayerPlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

@Deprecated
public class PlaceholderSystemManager {

    private static PlaceholderSystemManager instance;
    private final JavaPlugin plugin;

    private final Map<String, GlobalPlaceholder> globalPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, ContextPlaceholder> contextPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, PlayerPlaceholder> playerPlaceholders = new ConcurrentHashMap<>();

    private final Set<String> nonCacheablePlaceholders = ConcurrentHashMap.newKeySet();

    private final PlaceholderCache cache = new PlaceholderCache();

    @Setter
    private boolean debugMode = true;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    private PlaceholderSystemManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PlaceholderSystemManager(plugin);
            Placeholders.initialize(plugin);
        }
    }

    public static PlaceholderSystemManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("El sistema debe ser inicializado primero con initialize()");
        }
        return instance;
    }

    public String process(String text, Player player, Object... contexts) {
        return process(text, player, Arrays.asList(contexts));
    }

    public String process(String text, Player player, List<Object> contexts) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String cacheKey = generateCacheKey(text, player, contexts);
        if (cacheKey != null) {
            String cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        String result = text;

        result = processUnifiedPlaceholders(result, player, contexts);

        if (isPlaceholderAPIEnabled() && player != null) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                if (debugMode) {
                    logInternalWarn("Error procesando PlaceholderAPI: " + e.getMessage());
                }
            }
        }

        if (cacheKey != null) {
            cache.put(cacheKey, result);
        }

        return result;
    }

    public String process(String text, Player player) {
        return process(text, player, Collections.emptyList());
    }

    public String process(String text, Object... contexts) {
        return process(text, null, contexts);
    }

    public String process(String text) {
        return process(text, null, Collections.emptyList());
    }

    private String processUnifiedPlaceholders(String text, Player player, List<Object> contexts) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            String replacement = resolvePlaceholder(placeholderName, player, contexts);

            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String resolvePlaceholder(String placeholderName, Player player, List<Object> contexts) {
         
        for (Object context : contexts) {
            if (context instanceof ExyliaContext exyliaContext) {
                Object value = exyliaContext.get(placeholderName);
                if (value != null) {
                    return objectToString(value);
                }
            }
        }

        if (!contexts.isEmpty()) {
            ContextPlaceholder contextPlaceholder = contextPlaceholders.get(placeholderName);
            if (contextPlaceholder != null) {
                for (Object context : contexts) {
                    try {
                        Object result = contextPlaceholder.resolve(context, player);
                        if (result != null) {
                            return objectToString(result);
                        }
                    } catch (Exception e) {
                        if (debugMode) {
                            logInternalWarn("Error en placeholder de contexto '" + placeholderName + "': " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (player != null) {
            PlayerPlaceholder playerPlaceholder = playerPlaceholders.get(placeholderName);
            if (playerPlaceholder != null) {
                try {
                    Object result = playerPlaceholder.resolve(player);
                    if (result != null) {
                        return objectToString(result);
                    }
                } catch (Exception e) {
                    if (debugMode) {
                        logInternalWarn("Error en placeholder de jugador '" + placeholderName + "': " + e.getMessage());
                    }
                }
            }
        }

        GlobalPlaceholder globalPlaceholder = globalPlaceholders.get(placeholderName);
        if (globalPlaceholder != null) {
            try {
                Object result = globalPlaceholder.resolve();
                if (result != null) {
                    return objectToString(result);
                }
            } catch (Exception e) {
                if (debugMode) {
                    logInternalWarn("Error en placeholder global '" + placeholderName + "': " + e.getMessage());
                }
            }
        }

        return null;  
    }

    public void registerGlobal(String name, GlobalPlaceholder placeholder) {
        globalPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
        Placeholders.registerGlobal(name, (GlobalPlaceholderResolver) placeholder::resolve);
    }

    public void registerContext(String name, ContextPlaceholder placeholder) {
        contextPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
        Placeholders.registerContext(name, (context, player) -> {
            Object contextObj = extractFirstContextObject(context);
            return placeholder.resolve(contextObj, player);
        });
    }

    public void registerPlayer(String name, PlayerPlaceholder placeholder) {
        playerPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
        Placeholders.registerPlayer(name, (PlayerPlaceholderResolver) placeholder::resolve);
    }

    private Object extractFirstContextObject(PlaceholderContext context) {
        if (context == null) {
            return null;
        }
        return context;
    }

    public void registerGlobal(String name, Function<Void, Object> resolver) {
        registerGlobal(name, () -> resolver.apply(null));
    }

    public void registerContextLambda(String name, BiFunction<Object, Player, Object> resolver) {
        registerContext(name, resolver::apply);
    }

    public void registerPlayerLambda(String name, Function<Player, Object> resolver) {
        registerPlayer(name, resolver::apply);
    }

    public <T> void registerContextByType(String name, Class<T> contextType, Function<T, Object> resolver) {
        registerContext(name, (context, player) -> {
            T typedContext = findInContexts(Collections.singletonList(context), contextType);
            return typedContext != null ? resolver.apply(typedContext) : null;
        });
    }

    public <T> void registerContextByType(String name, Class<T> contextType, BiFunction<T, Player, Object> resolver) {
        registerContext(name, (context, player) -> {
            T typedContext = findInContexts(Collections.singletonList(context), contextType);
            return typedContext != null ? resolver.apply(typedContext, player) : null;
        });
    }

    public void registerGlobal(String name, GlobalPlaceholder placeholder, boolean cacheable) {
        registerGlobal(name, placeholder);
        if (!cacheable) {
            nonCacheablePlaceholders.add(name.toLowerCase());
        }
    }

    public void registerContext(String name, ContextPlaceholder placeholder, boolean cacheable) {
        registerContext(name, placeholder);
        if (!cacheable) {
            nonCacheablePlaceholders.add(name.toLowerCase());
        }
    }

    public void registerPlayer(String name, PlayerPlaceholder placeholder, boolean cacheable) {
        registerPlayer(name, placeholder);
        if (!cacheable) {
            nonCacheablePlaceholders.add(name.toLowerCase());
        }
    }

    public <T> void registerContextByType(String name, Class<T> contextType, Function<T, Object> resolver, boolean cacheable) {
        registerContextByType(name, contextType, resolver);
        if (!cacheable) {
            nonCacheablePlaceholders.add(name.toLowerCase());
        }
    }

    public <T> void registerContextByType(String name, Class<T> contextType, BiFunction<T, Player, Object> resolver, boolean cacheable) {
        registerContextByType(name, contextType, resolver);
        if (!cacheable) {
            nonCacheablePlaceholders.add(name.toLowerCase());
        }
    }

    private boolean containsNonCacheablePlaceholders(String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholderName = matcher.group(1).toLowerCase();
            if (nonCacheablePlaceholders.contains(placeholderName)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T findInContexts(List<Object> contexts, Class<T> type) {
        if (contexts == null || contexts.isEmpty()) {
            return null;
        }

        for (Object context : contexts) {
            if (context == null) continue;

            if (type.isInstance(context)) {
                return type.cast(context);
            }

            if (context instanceof List<?>) {
                for (Object item : (List<?>) context) {
                    if (type.isInstance(item)) {
                        return type.cast(item);
                    }
                }
            }

            if (context instanceof Object[]) {
                for (Object item : (Object[]) context) {
                    if (type.isInstance(item)) {
                        return type.cast(item);
                    }
                }
            }
        }

        return null;
    }

    public static <T> List<T> findAllInContexts(List<Object> contexts, Class<T> type) {
        List<T> results = new ArrayList<>();
        if (contexts == null || contexts.isEmpty()) {
            return results;
        }

        for (Object context : contexts) {
            if (context == null) continue;

            if (type.isInstance(context)) {
                results.add(type.cast(context));
            }

            if (context instanceof List<?>) {
                for (Object item : (List<?>) context) {
                    if (type.isInstance(item)) {
                        results.add(type.cast(item));
                    }
                }
            }

            if (context instanceof Object[]) {
                for (Object item : (Object[]) context) {
                    if (type.isInstance(item)) {
                        results.add(type.cast(item));
                    }
                }
            }
        }

        return results;
    }

    @FunctionalInterface
    public interface GlobalPlaceholder {
        Object resolve();
    }

    @FunctionalInterface
    public interface ContextPlaceholder {
        Object resolve(Object context, Player player);
    }

    @FunctionalInterface
    public interface PlayerPlaceholder {
        Object resolve(Player player);
    }

    private static class PlaceholderCache {
        private final Map<String, String> cache = new ConcurrentHashMap<>();
        private final Map<String, Long> timestamps = new ConcurrentHashMap<>();
        private static final long TTL = 1000;  

        public String get(String key) {
            Long timestamp = timestamps.get(key);
            if (timestamp == null || System.currentTimeMillis() - timestamp > TTL) {
                cache.remove(key);
                timestamps.remove(key);
                return null;
            }
            return cache.get(key);
        }

        public void put(String key, String value) {
            cache.put(key, value);
            timestamps.put(key, System.currentTimeMillis());
        }

        public void invalidatePattern(String pattern) {
            cache.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
            timestamps.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
        }

        public void clear() {
            cache.clear();
            timestamps.clear();
        }
    }

    private static String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        return obj.toString();
    }

    private String generateCacheKey(String text, Player player, List<Object> contexts) {
         
        if (!contexts.isEmpty() ||
                containsPlayerSpecificPlaceholders(text) ||
                containsNonCacheablePlaceholders(text)) {   
            return null;
        }

        String playerKey = player != null ? player.getUniqueId().toString() : "null";
        return text.hashCode() + "_" + playerKey;
    }

    private boolean containsPlayerSpecificPlaceholders(String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            if (playerPlaceholders.containsKey(placeholderName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPlaceholderAPI() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void clearAll() {
        globalPlaceholders.clear();
        contextPlaceholders.clear();
        playerPlaceholders.clear();
        cache.clear();
    }

    public PlaceholderStats getStats() {
        return new PlaceholderStats(
                globalPlaceholders.size(),
                contextPlaceholders.size(),
                playerPlaceholders.size(),
                cache.cache.size()
        );
    }

    public static class PlaceholderStats {
        public final int globalPlaceholders;
        public final int contextPlaceholders;
        public final int playerPlaceholders;
        public final int cachedEntries;

        PlaceholderStats(int global, int context, int player, int cached) {
            this.globalPlaceholders = global;
            this.contextPlaceholders = context;
            this.playerPlaceholders = player;
            this.cachedEntries = cached;
        }

        @Override
        public String toString() {
            return String.format("PlaceholderStats{global=%d, context=%d, player=%d, cached=%d}",
                    globalPlaceholders, contextPlaceholders, playerPlaceholders, cachedEntries);
        }
    }

    public void shutdown() {
        clearAll();
        instance = null;
    }
}
