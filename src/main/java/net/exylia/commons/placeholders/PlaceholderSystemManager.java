// ==================== SISTEMA CENTRALIZADO DE PLACEHOLDERS ====================

package net.exylia.commons.placeholders;

import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
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

/**
 * Sistema Centralizado de Placeholders para Exylia Commons
 * <p>
 * Este sistema unifica todos los placeholders en una sola arquitectura:
 * - Contextos múltiples con prioridad
 * - Placeholders globales, de contexto y de jugador
 * - Cache inteligente
 * - Integración automática con PlaceholderAPI
 * - API fluida y consistente
 */
public class PlaceholderSystemManager {

    private static PlaceholderSystemManager instance;
    private final JavaPlugin plugin;

    // Registro de placeholders por tipo
    private final Map<String, GlobalPlaceholder> globalPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, ContextPlaceholder> contextPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, PlayerPlaceholder> playerPlaceholders = new ConcurrentHashMap<>();

    // Cache para mejorar rendimiento
    private final PlaceholderCache cache = new PlaceholderCache();

    // Configuración
    @Setter
    private boolean debugMode = true;

    // Patrón para encontrar placeholders
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    private PlaceholderSystemManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa el sistema (debe llamarse una vez en onEnable)
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PlaceholderSystemManager(plugin);
        }
    }

    /**
     * Obtiene la instancia del sistema
     */
    public static PlaceholderSystemManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("El sistema debe ser inicializado primero con initialize()");
        }
        return instance;
    }

    // ==================== API PRINCIPAL DE PROCESAMIENTO ====================

    /**
     * Procesa un texto con contextos múltiples (API principal)
     * @param text Texto a procesar
     * @param player Jugador (puede ser null)
     * @param contexts Contextos en orden de prioridad (los primeros tienen mayor prioridad)
     * @return Texto procesado
     */
    public String process(String text, Player player, Object... contexts) {
        return process(text, player, Arrays.asList(contexts));
    }

    /**
     * Procesa un texto con contextos múltiples
     * @param text Texto a procesar
     * @param player Jugador (puede ser null)
     * @param contexts Lista de contextos en orden de prioridad
     * @return Texto procesado
     */
    public String process(String text, Player player, List<Object> contexts) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Generar clave de cache
        String cacheKey = generateCacheKey(text, player, contexts);
        if (cacheKey != null) {
            String cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        String result = text;

        // 1. Procesar placeholders del sistema unificado
        result = processUnifiedPlaceholders(result, player, contexts);

        // 2. Procesar PlaceholderAPI si está disponible
        if (isPlaceholderAPIEnabled() && player != null) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                if (debugMode) {
                    plugin.getLogger().warning("Error procesando PlaceholderAPI: " + e.getMessage());
                }
            }
        }

        // Guardar en cache si es válido
        if (cacheKey != null) {
            cache.put(cacheKey, result);
        }

        return result;
    }

    /**
     * Procesa solo con jugador (sin contextos adicionales)
     */
    public String process(String text, Player player) {
        return process(text, player, Collections.emptyList());
    }

    /**
     * Procesa solo con contextos (sin jugador)
     */
    public String process(String text, Object... contexts) {
        return process(text, null, contexts);
    }

    /**
     * Procesa texto sin contexto ni jugador
     */
    public String process(String text) {
        return process(text, null, Collections.emptyList());
    }

    // ==================== PROCESAMIENTO INTERNO ====================

    /**
     * Procesa los placeholders del sistema unificado
     */
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

    /**
     * Resuelve un placeholder específico
     */
    private String resolvePlaceholder(String placeholderName, Player player, List<Object> contexts) {
        // 1. Intentar con placeholders de contexto (tienen prioridad)
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
                            plugin.getLogger().warning("Error en placeholder de contexto '" + placeholderName + "': " + e.getMessage());
                        }
                    }
                }
            }
        }

        // 2. Intentar con placeholders de jugador
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
                        plugin.getLogger().warning("Error en placeholder de jugador '" + placeholderName + "': " + e.getMessage());
                    }
                }
            }
        }

        // 3. Intentar con placeholders globales
        GlobalPlaceholder globalPlaceholder = globalPlaceholders.get(placeholderName);
        if (globalPlaceholder != null) {
            try {
                Object result = globalPlaceholder.resolve();
                if (result != null) {
                    return objectToString(result);
                }
            } catch (Exception e) {
                if (debugMode) {
                    plugin.getLogger().warning("Error en placeholder global '" + placeholderName + "': " + e.getMessage());
                }
            }
        }

        return null; // No se encontró el placeholder
    }

    // ==================== REGISTRO DE PLACEHOLDERS ====================

    /**
     * Registra un placeholder global (no depende de nada)
     */
    public void registerGlobal(String name, GlobalPlaceholder placeholder) {
        globalPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
    }

    /**
     * Registra un placeholder de contexto (depende de un contexto)
     */
    public void registerContext(String name, ContextPlaceholder placeholder) {
        contextPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
    }

    /**
     * Registra un placeholder de jugador (solo depende del jugador)
     */
    public void registerPlayer(String name, PlayerPlaceholder placeholder) {
        playerPlaceholders.put(name.toLowerCase(), placeholder);
        cache.invalidatePattern(name);
    }

    // ==================== MÉTODOS DE CONVENIENCIA ====================

    /**
     * Registra un placeholder global con función lambda
     */
    public void registerGlobal(String name, Function<Void, Object> resolver) {
        registerGlobal(name, () -> resolver.apply(null));
    }

    /**
     * Registra un placeholder de contexto con función lambda
     */
    public void registerContextLambda(String name, BiFunction<Object, Player, Object> resolver) {
        registerContext(name, resolver::apply);
    }

    /**
     * Registra un placeholder de jugador con función lambda
     */
    public void registerPlayerLambda(String name, Function<Player, Object> resolver) {
        registerPlayer(name, resolver::apply);
    }

    /**
     * Registra un placeholder que busca automáticamente en contextos por tipo
     */
    public <T> void registerContextByType(String name, Class<T> contextType, Function<T, Object> resolver) {
        registerContext(name, (context, player) -> {
            T typedContext = findInContexts(Collections.singletonList(context), contextType);
            return typedContext != null ? resolver.apply(typedContext) : null;
        });
    }

    /**
     * Registra un placeholder que busca automáticamente en contextos por tipo con jugador
     */
    public <T> void registerContextByType(String name, Class<T> contextType, BiFunction<T, Player, Object> resolver) {
        registerContext(name, (context, player) -> {
            T typedContext = findInContexts(Collections.singletonList(context), contextType);
            return typedContext != null ? resolver.apply(typedContext, player) : null;
        });
    }

    // ==================== UTILIDADES DE BÚSQUEDA EN CONTEXTOS ====================

    /**
     * Busca un objeto de tipo específico en los contextos
     */
    public static <T> T findInContexts(List<Object> contexts, Class<T> type) {
        if (contexts == null || contexts.isEmpty()) {
            return null;
        }

        for (Object context : contexts) {
            if (context == null) continue;

            // Verificar si el contexto mismo es del tipo buscado
            if (type.isInstance(context)) {
                return type.cast(context);
            }

            // Si el contexto es una lista, buscar dentro
            if (context instanceof List<?>) {
                for (Object item : (List<?>) context) {
                    if (type.isInstance(item)) {
                        return type.cast(item);
                    }
                }
            }

            // Si el contexto es un array, buscar dentro
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

    /**
     * Busca todos los objetos de un tipo específico en los contextos
     */
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

    // ==================== INTERFACES PARA PLACEHOLDERS ====================

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

    // ==================== SISTEMA DE CACHE ====================

    private static class PlaceholderCache {
        private final Map<String, String> cache = new ConcurrentHashMap<>();
        private final Map<String, Long> timestamps = new ConcurrentHashMap<>();
        private static final long TTL = 30000; // 30 segundos

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

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Convierte un objeto a string manejando Components
     */
    private static String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        return obj.toString();
    }

    /**
     * Genera una clave de cache si es posible
     */
    private String generateCacheKey(String text, Player player, List<Object> contexts) {
        // Solo cachear si no hay contextos dinámicos
        if (!contexts.isEmpty() || containsPlayerSpecificPlaceholders(text)) {
            return null;
        }

        String playerKey = player != null ? player.getUniqueId().toString() : "null";
        return text.hashCode() + "_" + playerKey;
    }

    /**
     * Verifica si el texto contiene placeholders específicos del jugador
     */
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

    /**
     * Verifica si PlaceholderAPI está disponible
     */
    private boolean checkPlaceholderAPI() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== MÉTODOS DE GESTIÓN ====================

    /**
     * Limpia todos los placeholders registrados
     */
    public void clearAll() {
        globalPlaceholders.clear();
        contextPlaceholders.clear();
        playerPlaceholders.clear();
        cache.clear();
    }

    /**
     * Obtiene estadísticas del sistema
     */
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

    /**
     * Finalización del sistema
     */
    public void shutdown() {
        clearAll();
        instance = null;
    }
}