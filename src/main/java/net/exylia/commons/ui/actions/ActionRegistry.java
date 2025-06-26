
package net.exylia.commons.ui.actions;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Global action registry for menu items
 */
public class ActionRegistry {

    private static final Map<String, Consumer<ActionContext>> simpleActions = new HashMap<>();
    private static final Map<String, BiConsumer<ActionContext, String[]>> parameterizedActions = new HashMap<>();

    /**
     * Registers a simple action
     * @param name The action name
     * @param action The action handler
     */
    public static void registerAction(String name, Consumer<ActionContext> action) {
        simpleActions.put(name.toLowerCase(), action);
    }

    /**
     * Registers a parameterized action
     * @param name The action name
     * @param action The action handler
     */
    public static void registerAction(String name, BiConsumer<ActionContext, String[]> action) {
        parameterizedActions.put(name.toLowerCase(), action);
    }

    /**
     * Registers a player-only action
     * @param name The action name
     * @param action The action handler
     */
    public static void registerPlayerAction(String name, Consumer<Player> action) {
        registerAction(name, context -> action.accept(context.getPlayer()));
    }

    /**
     * Executes an action
     * @param actionString The action string
     * @param context The action context
     * @return True if the action was executed
     */
    public static boolean executeAction(String actionString, ActionContext context) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return false;
        }

        String[] parts = actionString.trim().split("\\s+", 2);
        String actionName = parts[0].toLowerCase();
        String[] parameters = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        // Try parameterized actions first
        BiConsumer<ActionContext, String[]> paramAction = parameterizedActions.get(actionName);
        if (paramAction != null) {
            try {
                paramAction.accept(context, parameters);
                return true;
            } catch (Exception e) {
                System.err.println("Error executing parameterized action '" + actionName + "': " + e.getMessage());
                return false;
            }
        }

        // Try simple actions
        Consumer<ActionContext> simpleAction = simpleActions.get(actionName);
        if (simpleAction != null) {
            try {
                simpleAction.accept(context);
                return true;
            } catch (Exception e) {
                System.err.println("Error executing simple action '" + actionName + "': " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * Checks if an action exists
     * @param actionName The action name
     * @return True if the action exists
     */
    public static boolean hasAction(String actionName) {
        String name = actionName.toLowerCase();
        return simpleActions.containsKey(name) || parameterizedActions.containsKey(name);
    }

    /**
     * Unregisters an action
     * @param actionName The action name
     */
    public static void unregisterAction(String actionName) {
        String name = actionName.toLowerCase();
        simpleActions.remove(name);
        parameterizedActions.remove(name);
    }

    /**
     * Clears all registered actions
     */
    public static void clear() {
        simpleActions.clear();
        parameterizedActions.clear();
    }

    /**
     * Gets action statistics
     * @return Map with action counts
     */
    public static Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("simple", simpleActions.size());
        stats.put("parameterized", parameterizedActions.size());
        stats.put("total", simpleActions.size() + parameterizedActions.size());
        return stats;
    }
}