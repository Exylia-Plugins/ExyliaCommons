package net.exylia.commons.ui.actions;

import net.exylia.commons.ui.events.MenuClickEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for action execution
 */
public class ActionContext {

    private final Player player;
    private final ActionSource source;
    private final Map<String, Object> data = new HashMap<>();

    public ActionContext(Player player, ActionSource source) {
        this.player = player;
        this.source = source;
    }

    /**
     * Creates an action context from a menu click event
     * @param event The menu click event
     * @return The action context
     */
    public static ActionContext fromMenuClick(MenuClickEvent event) {
        ActionContext context = new ActionContext(event.getPlayer(), ActionSource.MENU);
        context.put("menu", event.getMenu());
        context.put("item", event.getItem());
        context.put("slot", event.getSlot());
        context.put("clickType", event.getClickType());
        return context;
    }

    /**
     * Adds data to the context
     * @param key The key
     * @param value The value
     * @return This context for chaining
     */
    public ActionContext put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Gets data from the context
     * @param key The key
     * @param type The expected type
     * @param <T> The type
     * @return The value or null
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
     * Gets the player
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the action source
     * @return The source
     */
    public ActionSource getSource() {
        return source;
    }

    /**
     * Checks if data exists
     * @param key The key
     * @return True if data exists
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }
}