package net.exylia.commons.item.registry;

import net.exylia.commons.item.config.ItemConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRegistryImpl implements ItemRegistry {

    private final Map<String, ItemConfiguration> itemConfigurations = new ConcurrentHashMap<>();

    @Override
    public void registerItemConfiguration(String id, ItemConfiguration config) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (config == null) {
            throw new IllegalArgumentException("Item configuration cannot be null");
        }

        itemConfigurations.put(id.toLowerCase().trim(), config);
    }

    @Override
    public void registerItemConfigurations(ConfigurationSection configSection) {
        if (configSection == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        for (String itemId : configSection.getKeys(false)) {
            ConfigurationSection itemConfig = configSection.getConfigurationSection(itemId);
            if (itemConfig != null) {
                ItemConfiguration config = ItemConfiguration.builder()
                        .loadFromConfig(itemConfig)
                        .build();
                registerItemConfiguration(itemId, config);
            }
        }
    }

    @Override
    @Nullable
    public ItemConfiguration getItemConfiguration(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return itemConfigurations.get(id.toLowerCase().trim());
    }

    @Override
    public boolean hasItemConfiguration(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return itemConfigurations.containsKey(id.toLowerCase().trim());
    }

    @Override
    public void unregisterItemConfiguration(String id) {
        if (id != null && !id.trim().isEmpty()) {
            itemConfigurations.remove(id.toLowerCase().trim());
        }
    }

    @Override
    public void reloadItemConfiguration(String id, ItemConfiguration config) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (config == null) {
            throw new IllegalArgumentException("Item configuration cannot be null");
        }

        itemConfigurations.put(id.toLowerCase().trim(), config);
    }

    @Override
    public void reloadAllConfigurations(ConfigurationSection configSection) {
        clear();
        registerItemConfigurations(configSection);
    }

    @Override
    public Map<String, ItemConfiguration> getAllConfigurations() {
        return new ConcurrentHashMap<>(itemConfigurations);
    }

    @Override
    public void clear() {
        itemConfigurations.clear();
    }

    @Override
    public int size() {
        return itemConfigurations.size();
    }

    public String getStats() {
        long configurationsWithCooldown = itemConfigurations.values().stream()
                .mapToLong(config -> config.hasCooldown() ? 1 : 0)
                .sum();

        long configurationsWithRegions = itemConfigurations.values().stream()
                .mapToLong(config -> config.hasRegionConfiguration() ? 1 : 0)
                .sum();

        return String.format("Total configurations: %d, With cooldown: %d, With regions: %d",
                size(), configurationsWithCooldown, configurationsWithRegions);
    }
}
