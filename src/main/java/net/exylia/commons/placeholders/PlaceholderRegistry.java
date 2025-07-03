package net.exylia.commons.placeholders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PlaceholderRegistry {

    // Placeholders que solo necesitan el contexto
    private static final Map<String, Function<Object, Object>> contextPlaceholders = new HashMap<>();

    // Placeholders que necesitan el contexto y el jugador
    private static final Map<String, BiFunction<Object, Player, Object>> playerContextPlaceholders = new HashMap<>();

    // Placeholders que solo necesitan el jugador
    private static final Map<String, Function<Player, Object>> playerPlaceholders = new HashMap<>();

    // ========== MANEJO DE MÚLTIPLES CONTEXTOS ==========

    /**
     * Busca un objeto específico en el contexto (soporte para múltiples contextos)
     * @param context Contexto (puede ser un objeto único, un array, o una lista de objetos)
     * @param type Tipo de clase a buscar
     * @return Objeto del tipo especificado o null si no se encuentra
     */
    @SuppressWarnings("unchecked")
    public static <T> T findInContext(Object context, Class<T> type) {
        if (context == null) return null;

        // Si el contexto es una lista de objetos
        if (context instanceof List<?> contexts) {
            for (Object obj : contexts) {
                if (type.isInstance(obj)) {
                    return type.cast(obj);
                }
            }
        }
        // Si el contexto es un array de objetos
        else if (context instanceof Object[] contexts) {
            for (Object obj : contexts) {
                if (type.isInstance(obj)) {
                    return type.cast(obj);
                }
            }
        }
        // Si el contexto es un solo objeto
        else if (type.isInstance(context)) {
            return type.cast(context);
        }

        return null;
    }

    /**
     * Busca todos los objetos de un tipo específico en los contextos
     * @param context Contexto (puede ser un objeto único, un array, o una lista de objetos)
     * @param type Tipo de clase a buscar
     * @return Lista de objetos del tipo especificado
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> findAllInContext(Object context, Class<T> type) {
        List<T> results = new ArrayList<>();
        if (context == null) return results;

        // Si el contexto es una lista de objetos
        if (context instanceof List<?> contexts) {
            for (Object obj : contexts) {
                if (type.isInstance(obj)) {
                    results.add(type.cast(obj));
                }
            }
        }
        // Si el contexto es un array de objetos
        else if (context instanceof Object[] contexts) {
            for (Object obj : contexts) {
                if (type.isInstance(obj)) {
                    results.add(type.cast(obj));
                }
            }
        }
        // Si el contexto es un solo objeto
        else if (type.isInstance(context)) {
            results.add(type.cast(context));
        }

        return results;
    }

    /**
     * Obtiene todos los contextos como una lista unificada
     * @param context Contexto (puede ser un objeto único, un array, o una lista de objetos)
     * @return Lista de todos los objetos de contexto
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getAllContexts(Object context) {
        if (context == null) return new ArrayList<>();

        if (context instanceof List<?> contexts) {
            return new ArrayList<>((List<Object>) contexts);
        } else if (context instanceof Object[] contexts) {
            return Arrays.asList(contexts);
        } else {
            return Arrays.asList(context);
        }
    }

    // ========== MÉTODOS DE REGISTRO ==========

    /**
     * Registra un placeholder que solo usa el contexto
     * @param placeholder Nombre del placeholder (sin %)
     * @param replacer Función que recibe el contexto y devuelve el valor (String, Component o cualquier objeto)
     */
    public static void registerContext(String placeholder, Function<Object, Object> replacer) {
        contextPlaceholders.put(placeholder, replacer);
    }

    /**
     * Registra un placeholder que usa el contexto y el jugador
     * @param placeholder Nombre del placeholder (sin %)
     * @param replacer Función que recibe el contexto y el jugador, devuelve el valor (String, Component o cualquier objeto)
     */
    public static void registerPlayerContext(String placeholder, BiFunction<Object, Player, Object> replacer) {
        playerContextPlaceholders.put(placeholder, replacer);
    }

    /**
     * Registra un placeholder que solo usa el jugador
     * @param placeholder Nombre del placeholder (sin %)
     * @param replacer Función que recibe el jugador y devuelve el valor (String, Component o cualquier objeto)
     */
    public static void registerPlayer(String placeholder, Function<Player, Object> replacer) {
        playerPlaceholders.put(placeholder, replacer);
    }

    // ========== MÉTODOS DE PROCESAMIENTO ==========

    /**
     * Procesa placeholders con múltiples contextos (nuevo método para MessageBuilder)
     * Los contextos se procesan en orden, permitiendo que los más específicos sobrescriban a los generales
     */
    public static String processMultipleContexts(String message, List<Object> contexts, Player player) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;

        // Encontrar todos los placeholders en el texto
        Set<String> placeholdersInText = findPlaceholdersInText(message);

        // Para cada placeholder encontrado, buscar en todos los contextos
        for (String placeholderName : placeholdersInText) {
            String placeholder = "%" + placeholderName + "%";
            String replacement = null;

            // Buscar el placeholder en todos los contextos (en orden)
            for (Object context : contexts) {
                if (context == null) continue;

                // Intentar con placeholders de contexto
                if (contextPlaceholders.containsKey(placeholderName)) {
                    try {
                        Object value = contextPlaceholders.get(placeholderName).apply(context);
                        if (value != null && !value.toString().equals("N/A")) {
                            replacement = objectToString(value);
                            break; // Encontrado, usar este valor
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente contexto
                    }
                }

                // Intentar con placeholders de jugador + contexto
                if (replacement == null && playerContextPlaceholders.containsKey(placeholderName)) {
                    try {
                        Object value = playerContextPlaceholders.get(placeholderName).apply(context, player);
                        if (value != null && !value.toString().equals("N/A")) {
                            replacement = objectToString(value);
                            break; // Encontrado, usar este valor
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente contexto
                    }
                }
            }

            // Si no se encontró en ningún contexto, intentar con placeholders de solo jugador
            if (replacement == null && playerPlaceholders.containsKey(placeholderName)) {
                try {
                    Object value = playerPlaceholders.get(placeholderName).apply(player);
                    if (value != null) {
                        replacement = objectToString(value);
                    }
                } catch (Exception e) {
                    // Ignorar error
                }
            }

            // Si se encontró un reemplazo, aplicarlo
            if (replacement != null) {
                result = result.replace(placeholder, replacement);
            }
        }

        return result;
    }
    /**
     * Procesa placeholders con múltiples contextos usando varargs
     */
    public static String processMultipleContexts(String message, Player player, Object... contexts) {
        return processMultipleContexts(message, Arrays.asList(contexts), player);
    }

    /**
     * Procesa placeholders con múltiples contextos y sin jugador
     */
    public static String processMultipleContexts(String message, List<Object> contexts) {
        return processMultipleContexts(message, contexts, null);
    }

    /**
     * Procesa placeholders con contexto unificado (mantiene compatibilidad hacia atrás)
     * Este método funciona tanto con contextos únicos como múltiples
     */
    public static String process(String text, Object context, Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Si el contexto es una lista, usar el método de múltiples contextos
        if (context instanceof List<?>) {
            return processMultipleContexts(text, (List<Object>) context, player);
        }

        String result = text;
        boolean foundAnyPlaceholder = false;

        // Procesar placeholders de contexto
        for (Map.Entry<String, Function<Object, Object>> entry : contextPlaceholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    Object replacement = entry.getValue().apply(context);
                    String replacementStr = objectToString(replacement);
                    result = result.replace(placeholder, replacementStr);
                    foundAnyPlaceholder = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continúa con el siguiente placeholder
                }
            }
        }

        // Procesar placeholders de jugador + contexto
        for (Map.Entry<String, BiFunction<Object, Player, Object>> entry : playerContextPlaceholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    Object replacement = entry.getValue().apply(context, player);
                    String replacementStr = objectToString(replacement);
                    result = result.replace(placeholder, replacementStr);
                    foundAnyPlaceholder = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continúa con el siguiente placeholder
                }
            }
        }

        // Procesar placeholders de jugador
        for (Map.Entry<String, Function<Player, Object>> entry : playerPlaceholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    Object replacement = entry.getValue().apply(player);
                    String replacementStr = objectToString(replacement);
                    result = result.replace(placeholder, replacementStr);
                    foundAnyPlaceholder = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continúa con el siguiente placeholder
                }
            }
        }

        return result;
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    /**
     * Convierte un objeto a String manejando Components de Kyori
     * @param obj Objeto a convertir (puede ser String, Component o cualquier otro objeto)
     * @return String representation del objeto
     */
    private static String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        if (obj instanceof String string) {
            return string;
        }

        return obj.toString();
    }

    /**
     * Verifica si un placeholder está registrado
     */
    public static boolean isPlaceholderRegistered(String placeholder) {
        return contextPlaceholders.containsKey(placeholder) ||
                playerContextPlaceholders.containsKey(placeholder) ||
                playerPlaceholders.containsKey(placeholder);
    }

    /**
     * Obtiene todos los placeholders registrados
     */
    public static Set<String> getAllRegisteredPlaceholders() {
        Set<String> allPlaceholders = new HashSet<>();
        allPlaceholders.addAll(contextPlaceholders.keySet());
        allPlaceholders.addAll(playerContextPlaceholders.keySet());
        allPlaceholders.addAll(playerPlaceholders.keySet());
        return allPlaceholders;
    }

    /**
     * Obtiene todos los placeholders encontrados en un texto
     */
    public static Set<String> findPlaceholdersInText(String text) {
        Set<String> placeholders = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return placeholders;
        }

        // Buscar patrones %placeholder%
        int start = 0;
        while ((start = text.indexOf('%', start)) != -1) {
            int end = text.indexOf('%', start + 1);
            if (end != -1) {
                String placeholder = text.substring(start + 1, end);
                placeholders.add(placeholder);
                start = end + 1;
            } else {
                break;
            }
        }

        return placeholders;
    }

    /**
     * Limpia todos los placeholders registrados
     */
    public static void clear() {
        contextPlaceholders.clear();
        playerContextPlaceholders.clear();
        playerPlaceholders.clear();
    }

    /**
     * Obtiene estadísticas de placeholders registrados
     */
    public static Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("context", contextPlaceholders.size());
        stats.put("playerContext", playerContextPlaceholders.size());
        stats.put("player", playerPlaceholders.size());
        stats.put("total", contextPlaceholders.size() + playerContextPlaceholders.size() + playerPlaceholders.size());
        return stats;
    }

    /**
     * Obtiene información detallada de debug
     */
    public static String getDebugInfo() {
        Map<String, Integer> stats = getStats();
        return String.format("PlaceholderRegistry{contexto=%d, jugador+contexto=%d, jugador=%d, total=%d}",
                stats.get("context"),
                stats.get("playerContext"),
                stats.get("player"),
                stats.get("total"));
    }
}