package net.exylia.commons.v2.action.core;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistry {
    private final ConcurrentHashMap<String, Action> actions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JavaPlugin> owners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> namespaces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> simpleIdMappings = new ConcurrentHashMap<>();

    public void register(Action action) {
        if (action == null || action.getMetadata() == null) {
            throw new ActionException.ActionValidationException("Action and metadata cannot be null");
        }

        String fullId = action.getMetadata().getFullId();
        String simpleId = action.getMetadata().getId();

        if (actions.containsKey(fullId)) {
            DebugAPI.logLibWarn(DebugCategory.ACTION, "Action '" + fullId + "' is being replaced");
        }

        actions.put(fullId, action);
        owners.put(fullId, action.getMetadata().getOwner());

        String namespace = action.getMetadata().getNamespace();
        if (namespace != null) {
            namespaces.computeIfAbsent(namespace, k -> ConcurrentHashMap.newKeySet()).add(fullId);
        }

        registerSimpleId(simpleId, fullId);

        DebugAPI.logLibDebug(DebugCategory.ACTION, "Action registered: " + fullId);
    }

    public Optional<Action> get(String id) {
        return Optional.ofNullable(actions.get(id));
    }

    public Optional<Action> resolve(String id, String defaultNamespace) {
        if (id.contains(":")) {
            return get(id);
        }

        String mappedId = simpleIdMappings.get(id);
        if (mappedId != null) {
            return get(mappedId);
        }

        String namespaced = defaultNamespace + ":" + id;
        return get(namespaced).or(() -> get(id));
    }

    private void registerSimpleId(String simpleId, String fullId) {
        String existingMapping = simpleIdMappings.get(simpleId);

        if (existingMapping == null) {
            simpleIdMappings.put(simpleId, fullId);
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Simple ID '" + simpleId + "' mapped to '" + fullId + "'");
        } else if (!existingMapping.equals(fullId)) {
            simpleIdMappings.remove(simpleId);
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Simple ID '" + simpleId + "' has conflicts, namespace required. Conflicting actions: '" + existingMapping + "' and '" + fullId + "'");
        }
    }

    public void unregister(String id, JavaPlugin plugin) {
        JavaPlugin owner = owners.get(id);
        if (owner != null && !owner.equals(plugin)) {
            DebugAPI.logLibWarn(DebugCategory.ACTION, "Plugin " + plugin.getName() + " attempted to unregister action '" + id + "' owned by " + owner.getName());
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

            unregisterSimpleId(removed.getMetadata().getId(), id);

            DebugAPI.logLibDebug(DebugCategory.ACTION, "Action unregistered: " + id);
        }
    }

    private void unregisterSimpleId(String simpleId, String fullId) {
        String mappedId = simpleIdMappings.get(simpleId);
        if (fullId.equals(mappedId)) {
            simpleIdMappings.remove(simpleId);
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Simple ID mapping removed: " + simpleId);

            recheckSimpleIdConflict(simpleId);
        }
    }

    private void recheckSimpleIdConflict(String simpleId) {
        List<String> matchingActions = actions.keySet().stream()
            .filter(fullId -> {
                Action action = actions.get(fullId);
                return action != null && simpleId.equals(action.getMetadata().getId());
            })
            .toList();

        if (matchingActions.size() == 1) {
            simpleIdMappings.put(simpleId, matchingActions.get(0));
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Simple ID '" + simpleId + "' remapped to '" + matchingActions.get(0) + "' after unregistration");
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

        DebugAPI.logLibInfo(DebugCategory.ACTION, "Unregistered " + toRemove.size() + " actions for plugin: " + plugin.getName());
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
        int count = actions.size();
        actions.clear();
        owners.clear();
        namespaces.clear();
        simpleIdMappings.clear();
        DebugAPI.logLibInfo(DebugCategory.ACTION, "Action registry cleared (" + count + " actions removed)");
    }

    public int size() {
        return actions.size();
    }

    public boolean contains(String id) {
        return actions.containsKey(id);
    }
}
