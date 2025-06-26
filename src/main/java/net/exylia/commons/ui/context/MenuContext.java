// ==================== MENU CONTEXT SYSTEM ====================

package net.exylia.commons.ui.context;

import net.exylia.commons.placeholders.PlaceholderProcessor;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Context system for menus - handles placeholder processing and data storage
 * Now integrates with the global PlaceholderRegistry system
 */
public class MenuContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> typedData = new ConcurrentHashMap<>();
    private final PlaceholderProcessor placeholderProcessor;

    public MenuContext() {
        this.placeholderProcessor = new PlaceholderProcessor();
    }

    public MenuContext(PlaceholderProcessor processor) {
        this.placeholderProcessor = processor != null ? processor : new PlaceholderProcessor();
    }

    // ==================== DATA MANAGEMENT ====================

    /**
     * Stores data with a string key
     * @param key The key
     * @param value The value
     * @return This context for chaining
     */
    public MenuContext put(String key, Object value) {
        if (key != null) {
            data.put(key, value);
        }
        return this;
    }

    /**
     * Stores data with a type key
     * @param type The type class
     * @param value The value
     * @param <T> The type
     * @return This context for chaining
     */
    public <T> MenuContext put(Class<T> type, T value) {
        if (type != null) {
            typedData.put(type, value);
        }
        return this;
    }

    /**
     * Gets data by string key
     * @param key The key
     * @return The value, or null if not found
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Gets data by string key with type casting
     * @param key The key
     * @param type The expected type
     * @param <T> The type
     * @return The value cast to the type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets data by type
     * @param type The type class
     * @param <T> The type
     * @return The value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object value = typedData.get(type);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Checks if data exists for a key
     * @param key The key
     * @return True if data exists
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * Checks if data exists for a type
     * @param type The type
     * @return True if data exists
     */
    public boolean has(Class<?> type) {
        return typedData.containsKey(type);
    }

    /**
     * Removes data by key
     * @param key The key
     * @return This context for chaining
     */
    public MenuContext remove(String key) {
        data.remove(key);
        return this;
    }

    /**
     * Removes data by type
     * @param type The type
     * @return This context for chaining
     */
    public MenuContext remove(Class<?> type) {
        typedData.remove(type);
        return this;
    }

    /**
     * Clears all data
     * @return This context for chaining
     */
    public MenuContext clear() {
        data.clear();
        typedData.clear();
        return this;
    }

    // ==================== PLACEHOLDER PROCESSING ====================

    /**
     * Processes placeholders in text using both local and global placeholder systems
     * @param text The text to process
     * @param player The player (may be null)
     * @return The processed text
     */
    public String processPlaceholders(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // First, process with local PlaceholderProcessor
        String result = placeholderProcessor.process(text, this, player);

        // Then, process with global PlaceholderRegistry using our context data
        Object[] contextArray = buildContextArray();
        result = PlaceholderRegistry.process(result, contextArray, player);

        return result;
    }

    /**
     * Builds a context array compatible with PlaceholderRegistry
     * @return Array of all objects in this context
     */
    private Object[] buildContextArray() {
        List<Object> contextList = new ArrayList<>();

        // Add all typed data objects
        contextList.addAll(typedData.values());

        // Add all string-keyed data objects
        contextList.addAll(data.values());

        return contextList.toArray();
    }

    /**
     * Registers a placeholder processor for this context only
     * @param placeholder The placeholder name (without %)
     * @param processor The processor function
     * @return This context for chaining
     */
    public MenuContext registerPlaceholder(String placeholder, Function<MenuContext, Object> processor) {
        placeholderProcessor.register(placeholder, processor);
        return this;
    }

    /**
     * Registers a placeholder processor that uses both context and player
     * @param placeholder The placeholder name (without %)
     * @param processor The processor function
     * @return This context for chaining
     */
    public MenuContext registerPlaceholder(String placeholder, BiFunction<MenuContext, Player, Object> processor) {
        placeholderProcessor.register(placeholder, processor);
        return this;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Creates a copy of this context
     * @return A new context with copied data
     */
    public MenuContext copy() {
        MenuContext copy = new MenuContext(this.placeholderProcessor);
        copy.data.putAll(this.data);
        copy.typedData.putAll(this.typedData);
        return copy;
    }

    /**
     * Merges another context into this one
     * @param other The other context
     * @return This context for chaining
     */
    public MenuContext merge(MenuContext other) {
        if (other != null) {
            this.data.putAll(other.data);
            this.typedData.putAll(other.typedData);
        }
        return this;
    }

    /**
     * Gets all string keys
     * @return Set of string keys
     */
    public Set<String> getKeys() {
        return new HashSet<>(data.keySet());
    }

    /**
     * Gets all type keys
     * @return Set of type keys
     */
    public Set<Class<?>> getTypes() {
        return new HashSet<>(typedData.keySet());
    }

    /**
     * Gets the size of stored data
     * @return The total number of stored items
     */
    public int size() {
        return data.size() + typedData.size();
    }

    /**
     * Checks if the context is empty
     * @return True if no data is stored
     */
    public boolean isEmpty() {
        return data.isEmpty() && typedData.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MenuContext{");

        // Show typed data
        if (!typedData.isEmpty()) {
            sb.append("typedData=[");
            typedData.forEach((type, value) -> {
                sb.append(type.getSimpleName()).append(":").append(value).append(", ");
            });
            sb.append("], ");
        }

        // Show string data
        if (!data.isEmpty()) {
            sb.append("stringData=[");
            data.forEach((key, value) -> {
                sb.append(key).append(":").append(value).append(", ");
            });
            sb.append("], ");
        }

        sb.append("total=").append(size()).append("}");
        return sb.toString();
    }
}