package net.exylia.commons.v2.visual.core;

import lombok.Getter;
import net.exylia.commons.v2.visual.instance.GlobalCountdownInstance;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class GlobalVisualRegistry {
    private static volatile GlobalVisualRegistry instance;
    private static final Object LOCK = new Object();

    private final Map<String, GlobalCountdownInstance<?>> globalInstances = new ConcurrentHashMap<>();

    private GlobalVisualRegistry() {
    }

    public static GlobalVisualRegistry getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new GlobalVisualRegistry();
                }
            }
        }
        return instance;
    }

    public void register(String id, GlobalCountdownInstance<?> globalInstance) {
        globalInstances.put(id, globalInstance);
    }

    public void unregister(String id) {
        GlobalCountdownInstance<?> instance = globalInstances.remove(id);
        if (instance != null) {
            instance.cancel();
        }
    }

    public Optional<GlobalCountdownInstance<?>> get(String id) {
        return Optional.ofNullable(globalInstances.get(id));
    }

    public Map<String, GlobalCountdownInstance<?>> getAll() {
        return Map.copyOf(globalInstances);
    }

    public void clear() {
        globalInstances.values().forEach(GlobalCountdownInstance::cancel);
        globalInstances.clear();
    }

    public int getGlobalInstanceCount() {
        return globalInstances.size();
    }
}
