// GlobalActionManager.java
package net.exylia.commons.actions;

import net.exylia.commons.item.exceptions.ItemException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class GlobalActionManager {
    private static final Map<String, BiConsumer<ActionContext, String[]>> actions = new HashMap<>();
    private static final Map<String, JavaPlugin> actionOwners = new HashMap<>();

    /**
     * Registra una nueva acción personalizada
     * @param actionName Nombre único de la acción (ej: "show_help", "open_shop")
     * @param plugin Plugin que registra la acción
     * @param handler Función que ejecuta la acción. Recibe ActionContext y argumentos opcionales
     */
    public static void registerAction(String actionName, JavaPlugin plugin, BiConsumer<ActionContext, String[]> handler) {
        if (actionName == null || actionName.trim().isEmpty()) {
            logInternalWarn("Cannot register action with null or empty name");
            return;
        }

        if (handler == null) {
            logInternalWarn("Cannot register action '" + actionName + "' with null handler");
            return;
        }

        String normalizedName = actionName.toLowerCase().trim();

        // Verificar si ya existe
        if (actions.containsKey(normalizedName)) {
            JavaPlugin existingOwner = actionOwners.get(normalizedName);
            logInternalWarn("Action '" + actionName + "' is already registered by " +
                    (existingOwner != null ? existingOwner.getName() : "unknown plugin") +
                    ". Overriding with " + plugin.getName());
        }

        actions.put(normalizedName, handler);
        actionOwners.put(normalizedName, plugin);
    }

    /**
     * Registra una acción simple que solo necesita el jugador
     * @param actionName Nombre de la acción
     * @param plugin Plugin que registra la acción
     * @param playerHandler Función que recibe solo el jugador
     */
    public static void registerPlayerAction(String actionName, JavaPlugin plugin, java.util.function.Consumer<Player> playerHandler) {
        registerAction(actionName, plugin, (context, args) -> playerHandler.accept(context.getPlayer()));
    }

    /**
     * Ejecuta una acción si existe
     * @param actionString String de acción completo (ej: "show_help arg1 arg2")
     * @param context Contexto de la acción
     * @return true si la acción fue ejecutada, false si no existe
     */
    public static boolean executeAction(String actionString, ActionContext context) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return false;
        }

        // Parsear la acción y argumentos
        String[] parts = actionString.trim().split("\\s+");
        String actionName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        // Buscar y ejecutar la acción
        BiConsumer<ActionContext, String[]> handler = actions.get(actionName);
        if (handler != null) {
            try {
                handler.accept(context, args);
                return true;
            } catch (ItemException e) {
                JavaPlugin owner = actionOwners.get(actionName);
                logInternalWarn("Error executing action '" + actionName + "' from plugin " +
                        (owner != null ? owner.getName() : "unknown") + ": " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * Verifica si una acción está registrada
     * @param actionName Nombre de la acción
     * @return true si la acción existe
     */
    public static boolean hasAction(String actionName) {
        return actionName != null && actions.containsKey(actionName.toLowerCase().trim());
    }

    /**
     * Desregistra una acción
     * @param actionName Nombre de la acción a desregistrar
     * @param plugin Plugin que intenta desregistrar (debe ser el mismo que la registró)
     * @return true si fue desregistrada exitosamente
     */
    public static boolean unregisterAction(String actionName, JavaPlugin plugin) {
        if (actionName == null) return false;

        String normalizedName = actionName.toLowerCase().trim();
        JavaPlugin owner = actionOwners.get(normalizedName);

        // Verificar que el plugin sea el propietario
        if (owner != null && !owner.equals(plugin)) {
            logInternalWarn("Plugin " + plugin.getName() + " tried to unregister action '" +
                    actionName + "' owned by " + owner.getName());
            return false;
        }

        actions.remove(normalizedName);
        actionOwners.remove(normalizedName);
        return true;
    }

    /**
     * Desregistra todas las acciones de un plugin específico
     * @param plugin Plugin cuyas acciones se van a desregistrar
     * @return Número de acciones desregistradas
     */
    public static int unregisterPluginActions(JavaPlugin plugin) {
        if (plugin == null) return 0;

        int count = 0;
        var iterator = actionOwners.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (plugin.equals(entry.getValue())) {
                actions.remove(entry.getKey());
                iterator.remove();
                count++;
            }
        }

        return count;
    }

    public static Map<String, JavaPlugin> getRegisteredActions() {
        return new HashMap<>(actionOwners);
    }
    public static void clearAllActions() {
        actions.clear();
        actionOwners.clear();
    }
}