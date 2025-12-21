package net.exylia.commons.v2.ui.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.Getter;
import net.exylia.commons.v2.ui.config.MenuConfig;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.MenuV2;
import net.exylia.commons.v2.ui.navigation.MenuHistory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class MenuCacheManager {
    private final Cache<String, MenuV2> templateCache;
    private final Cache<String, MenuV2> instanceCache;
    private final Cache<String, MenuConfig> configCache;
    private final Cache<UUID, MenuHistory> historyCache;

    public MenuCacheManager() {
        this.templateCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.instanceCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .removalListener((String key, MenuV2 menu, RemovalCause cause) -> {
                    if (menu != null && menu.getState() == MenuState.OPEN) {
                        menu.close();
                    }
                })
                .recordStats()
                .build();

        this.configCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.historyCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public void cacheTemplate(String id, MenuV2 menu) {
        templateCache.put(id, menu);
    }

    public Optional<MenuV2> getTemplate(String id) {
        return Optional.ofNullable(templateCache.getIfPresent(id));
    }

    public void invalidateTemplate(String id) {
        templateCache.invalidate(id);
    }

    public void cacheInstance(String instanceId, MenuV2 menu) {
        instanceCache.put(instanceId, menu);
    }

    public Optional<MenuV2> getInstance(String instanceId) {
        return Optional.ofNullable(instanceCache.getIfPresent(instanceId));
    }

    public void invalidateInstance(String instanceId) {
        instanceCache.invalidate(instanceId);
    }

    public void cacheConfig(String fileName, MenuConfig config) {
        configCache.put(fileName, config);
    }

    public Optional<MenuConfig> getConfig(String fileName) {
        return Optional.ofNullable(configCache.getIfPresent(fileName));
    }

    public void invalidateConfig(String fileName) {
        configCache.invalidate(fileName);
    }

    public void cacheHistory(UUID playerId, MenuHistory history) {
        historyCache.put(playerId, history);
    }

    public Optional<MenuHistory> getHistory(UUID playerId) {
        return Optional.ofNullable(historyCache.getIfPresent(playerId));
    }

    public void invalidateHistory(UUID playerId) {
        historyCache.invalidate(playerId);
    }

    public void invalidateAll() {
        templateCache.invalidateAll();
        instanceCache.invalidateAll();
        configCache.invalidateAll();
        historyCache.invalidateAll();
    }

    public void invalidateAllConfigs() {
        configCache.invalidateAll();
    }

    public void cleanupPlayer(UUID playerId) {
        historyCache.invalidate(playerId);

        instanceCache.asMap().entrySet().removeIf(entry -> {
            MenuV2 menu = entry.getValue();
            return menu.getViewer() != null && menu.getViewer().getUniqueId().equals(playerId);
        });
    }

    public UICacheStats getStats() {
        return UICacheStats.builder()
                .templateCacheStats(templateCache.stats())
                .instanceCacheStats(instanceCache.stats())
                .configCacheStats(configCache.stats())
                .historyCacheStats(historyCache.stats())
                .templateCacheSize(templateCache.estimatedSize())
                .instanceCacheSize(instanceCache.estimatedSize())
                .configCacheSize(configCache.estimatedSize())
                .historyCacheSize(historyCache.estimatedSize())
                .build();
    }
}
