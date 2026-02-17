package net.exylia.commons.v2.ui.packet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ContainerIdTracker {

    private static final Map<UUID, Integer> containerIds = new ConcurrentHashMap<>();

    private ContainerIdTracker() {}

    public static void setContainerId(UUID playerId, int containerId) {
        containerIds.put(playerId, containerId);
    }

    public static int getContainerId(UUID playerId) {
        return containerIds.getOrDefault(playerId, -1);
    }

    public static void remove(UUID playerId) {
        containerIds.remove(playerId);
    }

    public static void clear() {
        containerIds.clear();
    }
}
