package net.exylia.commons.v2.visual.core;

import net.exylia.commons.v2.visual.instance.VisualInstance;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VisualRegistry {
    private static volatile VisualRegistry instance;
    private static final Object LOCK = new Object();

    private final Map<UUID, Map<String, VisualInstance<?>>> playerInstances;
    private final Map<String, VisualType> instanceTypes;

    private VisualRegistry() {
        this.playerInstances = new ConcurrentHashMap<>();
        this.instanceTypes = new ConcurrentHashMap<>();
    }

    public static VisualRegistry getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new VisualRegistry();
                }
            }
        }
        return instance;
    }

    public void register(UUID playerId, String instanceId, VisualInstance<?> instance, VisualType type) {
        playerInstances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(instanceId, instance);
        instanceTypes.put(instanceId, type);
    }

    public void remove(UUID playerId, String instanceId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        if (instances != null) {
            instances.remove(instanceId);
            if (instances.isEmpty()) {
                playerInstances.remove(playerId);
            }
        }
        instanceTypes.remove(instanceId);
    }

    public Optional<VisualInstance<?>> get(UUID playerId, String instanceId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        return Optional.ofNullable(instances != null ? instances.get(instanceId) : null);
    }

    public List<VisualInstance<?>> getByPlayer(Player player) {
        return getByPlayer(player.getUniqueId());
    }

    public List<VisualInstance<?>> getByPlayer(UUID playerId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        return instances != null ? new ArrayList<>(instances.values()) : Collections.emptyList();
    }

    public List<VisualInstance<?>> getByPlayerAndType(Player player, VisualType type) {
        return getByPlayer(player).stream()
                .filter(instance -> instanceTypes.get(instance.getId()) == type)
                .collect(Collectors.toList());
    }

    public int countByPlayer(Player player) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(player.getUniqueId());
        return instances != null ? instances.size() : 0;
    }

    public int countByPlayerAndType(Player player, VisualType type) {
        return (int) getByPlayer(player).stream()
                .filter(instance -> instanceTypes.get(instance.getId()) == type)
                .count();
    }

    public void removeAllByPlayer(UUID playerId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        if (instances != null) {
            new ArrayList<>(instances.values()).forEach(VisualInstance::cancel);
        }

        playerInstances.remove(playerId);

        if (instances != null) {
            for (String instanceId : instances.keySet()) {
                instanceTypes.remove(instanceId);
            }
        }
    }

    public void removeAllByPlayer(Player player) {
        removeAllByPlayer(player.getUniqueId());
    }

    public List<VisualInstance<?>> getAllInstances() {
        return playerInstances.values().stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    public int size() {
        return instanceTypes.size();
    }

    public void clear() {
        List<VisualInstance<?>> instances = getAllInstances();
        instances.forEach(VisualInstance::cancel);
        playerInstances.clear();
        instanceTypes.clear();
    }

    public boolean has(UUID playerId, String instanceId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        return instances != null && instances.containsKey(instanceId);
    }

    public Optional<VisualType> getType(String instanceId) {
        return Optional.ofNullable(instanceTypes.get(instanceId));
    }
}
