package net.exylia.commons.placeholders;

import net.exylia.commons.ui.context.MenuContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;

/**
 * Advanced placeholder processor for the UI system
 * Handles context-based placeholders and integrates with PlaceholderAPI
 */
public class PlaceholderProcessor {

    private final Map<String, Function<MenuContext, Object>> contextProcessors = new HashMap<>();
    private final Map<String, BiFunction<MenuContext, Player, Object>> playerContextProcessors = new HashMap<>();

    /**
     * Registers a context-only placeholder processor
     * @param placeholder The placeholder name (without %)
     * @param processor The processor function
     */
    public void register(String placeholder, Function<MenuContext, Object> processor) {
        contextProcessors.put(placeholder.toLowerCase(), processor);
    }

    /**
     * Registers a context + player placeholder processor
     * @param placeholder The placeholder name (without %)
     * @param processor The processor function
     */
    public void register(String placeholder, BiFunction<MenuContext, Player, Object> processor) {
        playerContextProcessors.put(placeholder.toLowerCase(), processor);
    }

    /**
     * Processes placeholders in text
     * @param text The text to process
     * @param context The menu context (may be null)
     * @param player The player (may be null)
     * @return The processed text
     */
    public String process(String text, MenuContext context, Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Process context-only placeholders
        for (Map.Entry<String, Function<MenuContext, Object>> entry : contextProcessors.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    Object replacement = entry.getValue().apply(context);
                    String replacementStr = objectToString(replacement);
                    result = result.replace(placeholder, replacementStr);
                } catch (Exception e) {
                    // Log error but continue processing
                    System.err.println("Error processing placeholder " + placeholder + ": " + e.getMessage());
                }
            }
        }

        // Process context + player placeholders
        for (Map.Entry<String, BiFunction<MenuContext, Player, Object>> entry : playerContextProcessors.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    Object replacement = entry.getValue().apply(context, player);
                    String replacementStr = objectToString(replacement);
                    result = result.replace(placeholder, replacementStr);
                } catch (Exception e) {
                    // Log error but continue processing
                    System.err.println("Error processing placeholder " + placeholder + ": " + e.getMessage());
                }
            }
        }

        // Process PlaceholderAPI placeholders last
        if (isPlaceholderAPIEnabled() && player != null) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }

        return result;
    }

    /**
     * Converts an object to string, handling Components properly
     * @param obj The object to convert
     * @return String representation
     */
    private String objectToString(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        return obj.toString();
    }

    /**
     * Clears all registered processors
     */
    public void clear() {
        contextProcessors.clear();
        playerContextProcessors.clear();
    }

    /**
     * Gets processor statistics
     * @return Map with processor counts
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("context", contextProcessors.size());
        stats.put("playerContext", playerContextProcessors.size());
        stats.put("total", contextProcessors.size() + playerContextProcessors.size());
        return stats;
    }
}