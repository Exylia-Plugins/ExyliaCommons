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
    private final Map<UUID, Map<String, VisualType>> instanceTypes;

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
        instanceTypes.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(instanceId, type);
    }

    public void remove(UUID playerId, String instanceId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        if (instances != null) {
            instances.remove(instanceId);
            if (instances.isEmpty()) {
                playerInstances.remove(playerId);
            }
        }
        Map<String, VisualType> types = instanceTypes.get(playerId);
        if (types != null) {
            types.remove(instanceId);
            if (types.isEmpty()) {
                instanceTypes.remove(playerId);
            }
        }
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
        UUID playerId = player.getUniqueId();
        Map<String, VisualType> types = instanceTypes.get(playerId);
        if (types == null) return Collections.emptyList();
        return getByPlayer(playerId).stream()
                .filter(instance -> types.get(instance.getId()) == type)
                .collect(Collectors.toList());
    }

    public int countByPlayer(Player player) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(player.getUniqueId());
        return instances != null ? instances.size() : 0;
    }

    public int countByPlayerAndType(Player player, VisualType type) {
        return getByPlayerAndType(player, type).size();
    }

    public void removeAllByPlayer(UUID playerId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        if (instances != null) {
            new ArrayList<>(instances.values()).forEach(VisualInstance::cancel);
        }

        playerInstances.remove(playerId);
        instanceTypes.remove(playerId);
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
        return (int) playerInstances.values().stream().mapToLong(Map::size).sum();
    }

    public void clear() {
        getAllInstances().forEach(VisualInstance::cancel);
        playerInstances.clear();
        instanceTypes.clear();
    }

    public boolean has(UUID playerId, String instanceId) {
        Map<String, VisualInstance<?>> instances = playerInstances.get(playerId);
        return instances != null && instances.containsKey(instanceId);
    }

    public Optional<VisualType> getType(UUID playerId, String instanceId) {
        Map<String, VisualType> types = instanceTypes.get(playerId);
        return Optional.ofNullable(types != null ? types.get(instanceId) : null);
    }
}
