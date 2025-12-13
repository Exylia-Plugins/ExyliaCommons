package net.exylia.commons.v2.action.core;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistry {
    private final ConcurrentHashMap<String, Action> actions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JavaPlugin> owners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> namespaces = new ConcurrentHashMap<>();

    public void register(Action action) {
        if (action == null || action.getMetadata() == null) {
            throw new ActionException.ActionValidationException("Action and metadata cannot be null");
        }

        String fullId = action.getMetadata().getFullId();

        if (actions.containsKey(fullId)) {
            DebugUtils.logInternalWarn("Action '" + fullId + "' is being replaced");
        }

        actions.put(fullId, action);
        owners.put(fullId, action.getMetadata().getOwner());

        String namespace = action.getMetadata().getNamespace();
        if (namespace != null) {
            namespaces.computeIfAbsent(namespace, k -> ConcurrentHashMap.newKeySet()).add(fullId);
        }

        DebugUtils.logInternalInfo("Action registered: " + fullId);
    }

    public Optional<Action> get(String id) {
        return Optional.ofNullable(actions.get(id));
    }

    public Optional<Action> resolve(String id, String defaultNamespace) {
        if (id.contains(":")) {
            return get(id);
        }

        String namespaced = defaultNamespace + ":" + id;
        return get(namespaced).or(() -> get(id));
    }

    public void unregister(String id, JavaPlugin plugin) {
        JavaPlugin owner = owners.get(id);
        if (owner != null && !owner.equals(plugin)) {
            DebugUtils.logInternalWarn("Plugin " + plugin.getName() + " attempted to unregister action '" + id + "' owned by " + owner.getName());
            return;
        }

        Action removed = actions.remove(id);
        owners.remove(id);

        if (removed != null) {
            String namespace = removed.getMetadata().getNamespace();
            if (namespace != null) {
                Set<String> namespaceActions = namespaces.get(namespace);
                if (namespaceActions != null) {
                    namespaceActions.remove(id);
                }
            }
            DebugUtils.logInternalInfo("Action unregistered: " + id);
        }
    }

    public void unregisterByOwner(JavaPlugin plugin) {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, JavaPlugin> entry : owners.entrySet()) {
            if (entry.getValue().equals(plugin)) {
                toRemove.add(entry.getKey());
            }
        }

        for (String id : toRemove) {
            unregister(id, plugin);
        }

        DebugUtils.logInternalInfo("Unregistered " + toRemove.size() + " actions for plugin: " + plugin.getName());
    }

    public Collection<Action> getAll() {
        return Collections.unmodifiableCollection(actions.values());
    }

    public Set<String> getNamespaces() {
        return Collections.unmodifiableSet(namespaces.keySet());
    }

    public Collection<Action> getAllByNamespace(String namespace) {
        Set<String> actionIds = namespaces.get(namespace);
        if (actionIds == null) {
            return Collections.emptyList();
        }

        List<Action> result = new ArrayList<>();
        for (String id : actionIds) {
            Action action = actions.get(id);
            if (action != null) {
                result.add(action);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public void clear() {
        actions.clear();
        owners.clear();
        namespaces.clear();
        DebugUtils.logInternalInfo("Action registry cleared");
    }

    public int size() {
        return actions.size();
    }

    public boolean contains(String id) {
        return actions.containsKey(id);
    }
}
