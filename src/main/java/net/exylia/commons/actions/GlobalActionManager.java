package net.exylia.commons.actions;

import net.exylia.commons.item.exceptions.ItemException;
import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.action.migration.ActionV1Adapter;
import net.exylia.commons.v2.action.migration.MigrationHelper;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionMetadata;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.parser.ArgumentToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

@Deprecated
public class GlobalActionManager {
    private static final Map<String, BiConsumer<ActionContext, String[]>> actions = new HashMap<>();
    private static final Map<String, JavaPlugin> actionOwners = new HashMap<>();

    @Deprecated
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

        if (ActionAPI.isInitialized()) {
            ActionMetadata metadata = ActionMetadata.builder()
                    .id(normalizedName)
                    .namespace(plugin.getName().toLowerCase())
                    .owner(plugin)
                    .build();

            Action v2Action = new ActionV1Adapter(metadata, handler);
            ActionAPI.register(v2Action);
        } else {
            if (actions.containsKey(normalizedName)) {
                JavaPlugin existingOwner = actionOwners.get(normalizedName);
                logInternalWarn("Action '" + actionName + "' is already registered by " +
                        (existingOwner != null ? existingOwner.getName() : "unknown plugin") +
                        ". Overriding with " + plugin.getName());
            }

            actions.put(normalizedName, handler);
            actionOwners.put(normalizedName, plugin);
        }
    }

    @Deprecated
    public static void registerPlayerAction(String actionName, JavaPlugin plugin, java.util.function.Consumer<Player> playerHandler) {
        registerAction(actionName, plugin, (context, args) -> playerHandler.accept(context.getPlayer()));
    }

    @Deprecated
    public static boolean executeAction(String actionString, ActionContext context) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return false;
        }

        if (ActionAPI.isInitialized()) {
            try {
                net.exylia.commons.v2.action.model.ActionContext v2Context = MigrationHelper.convertContextV1ToV2(context);
                ActionResult result = ActionAPI.execute(actionString, v2Context);
                return result.isSuccess();
            } catch (Exception e) {
                logInternalWarn("Error executing action via V2: " + e.getMessage());
                return false;
            }
        }

        String[] parts = actionString.trim().split("\\s+");
        String actionName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

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

    @Deprecated
    public static boolean hasAction(String actionName) {
        if (ActionAPI.isInitialized()) {
            return ActionAPI.get(actionName).isPresent();
        }
        return actionName != null && actions.containsKey(actionName.toLowerCase().trim());
    }

    @Deprecated
    public static boolean unregisterAction(String actionName, JavaPlugin plugin) {
        if (actionName == null) return false;

        String normalizedName = actionName.toLowerCase().trim();

        if (ActionAPI.isInitialized()) {
            ActionAPI.unregister(normalizedName, plugin);
            return true;
        }

        JavaPlugin owner = actionOwners.get(normalizedName);

        if (owner != null && !owner.equals(plugin)) {
            logInternalWarn("Plugin " + plugin.getName() + " tried to unregister action '" +
                    actionName + "' owned by " + owner.getName());
            return false;
        }

        actions.remove(normalizedName);
        actionOwners.remove(normalizedName);
        return true;
    }

    @Deprecated
    public static int unregisterPluginActions(JavaPlugin plugin) {
        if (plugin == null) return 0;

        if (ActionAPI.isInitialized()) {
            ActionAPI.unregisterAll(plugin);
            return 0;
        }

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

    @Deprecated
    public static Map<String, JavaPlugin> getRegisteredActions() {
        if (ActionAPI.isInitialized()) {
            return ActionAPI.getAll().stream()
                    .collect(Collectors.toMap(
                            action -> action.getMetadata().getFullId(),
                            action -> action.getMetadata().getOwner()
                    ));
        }
        return new HashMap<>(actionOwners);
    }

    @Deprecated
    public static void clearAllActions() {
        if (ActionAPI.isInitialized()) {
            ActionAPI.getManager().clearAll();
        }
        actions.clear();
        actionOwners.clear();
    }
}
