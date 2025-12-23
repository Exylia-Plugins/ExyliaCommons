package net.exylia.commons.v2.hologram.update;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.cache.HologramCacheManager;
import net.exylia.commons.v2.hologram.core.HologramRegistry;
import net.exylia.commons.v2.hologram.model.Hologram;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@RequiredArgsConstructor
public class UpdateScheduler {
    private final HologramRegistry registry;
    private final HologramCacheManager cacheManager;
    private final Map<Long, Set<String>> intervalGroups = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public void updateAll() {
        currentTick++;

        intervalGroups.entrySet().stream()
                .filter(entry -> currentTick % entry.getKey() == 0)
                .flatMap(entry -> entry.getValue().stream())
                .forEach(hologramId -> {
                    registry.get(hologramId).ifPresent(hologram -> {
                        if (hologram.isSpawned() && hologram.getConfig().shouldUpdate()) {
                            hologram.updateAsync().exceptionally(ex -> {
                                return null;
                            });
                        }
                    });
                });
    }

    private boolean shouldUpdate(Hologram hologram) {
        if (!hologram.isSpawned()) {
            return false;
        }

        if (!hologram.getConfig().shouldUpdate()) {
            return false;
        }

        long interval = hologram.getConfig().getUpdateInterval();
        if (interval <= 0) {
            return false;
        }

        return currentTick % interval == 0;
    }

    public void scheduleUpdate(Hologram hologram) {
        long interval = hologram.getConfig().getUpdateInterval();
        if (interval > 0) {
            intervalGroups.computeIfAbsent(interval, k -> new CopyOnWriteArraySet<>())
                    .add(hologram.getId());
        }
    }

    public void unscheduleUpdate(String hologramId) {
        intervalGroups.values().forEach(set -> set.remove(hologramId));
    }

    public void clear() {
        intervalGroups.clear();
        currentTick = 0;
    }

    public long getCurrentTick() {
        return currentTick;
    }
}
