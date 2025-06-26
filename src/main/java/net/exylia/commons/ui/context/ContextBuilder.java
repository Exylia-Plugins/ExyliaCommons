package net.exylia.commons.ui.context;

import net.exylia.commons.placeholders.PlaceholderProcessor;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Builder for creating MenuContext instances
 */
class ContextBuilder {

    private final MenuContext context;

    public ContextBuilder() {
        this.context = new MenuContext();
    }

    public ContextBuilder(PlaceholderProcessor processor) {
        this.context = new MenuContext(processor);
    }

    /**
     * Adds data to the context
     * @param key The key
     * @param value The value
     * @return This builder for chaining
     */
    public ContextBuilder with(String key, Object value) {
        context.put(key, value);
        return this;
    }

    /**
     * Adds typed data to the context
     * @param type The type
     * @param value The value
     * @param <T> The type
     * @return This builder for chaining
     */
    public <T> ContextBuilder with(Class<T> type, T value) {
        context.put(type, value);
        return this;
    }

    /**
     * Adds a placeholder processor
     * @param placeholder The placeholder name
     * @param processor The processor
     * @return This builder for chaining
     */
    public ContextBuilder withPlaceholder(String placeholder, Function<MenuContext, Object> processor) {
        context.registerPlaceholder(placeholder, processor);
        return this;
    }

    /**
     * Adds a placeholder processor with player support
     * @param placeholder The placeholder name
     * @param processor The processor
     * @return This builder for chaining
     */
    public ContextBuilder withPlaceholder(String placeholder, BiFunction<MenuContext, Player, Object> processor) {
        context.registerPlaceholder(placeholder, processor);
        return this;
    }

    /**
     * Builds the context
     * @return The built context
     */
    public MenuContext build() {
        return context;
    }
}