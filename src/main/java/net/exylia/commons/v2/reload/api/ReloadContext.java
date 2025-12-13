package net.exylia.commons.v2.reload.api;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.reload.stats.ReloadStats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
public class ReloadContext {
    private final ExyliaPlugin plugin;
    private final Map<String, Object> data;
    private final Set<String> reloadedSystems;
    private ReloadStats partialStats;

    public ReloadContext(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.data = new HashMap<>();
        this.reloadedSystems = new HashSet<>();
    }

    public boolean wasReloaded(String systemName) {
        return reloadedSystems.contains(systemName);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public void markReloaded(String systemName) {
        reloadedSystems.add(systemName);
    }

    public void setPartialStats(ReloadStats stats) {
        this.partialStats = stats;
    }
}
