package net.exylia.commons.item.registry;

import net.exylia.commons.item.config.ItemConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ItemRegistry {

    void registerItemConfiguration(String id, ItemConfiguration config);

    void registerItemConfigurations(ConfigurationSection configSection);

    @Nullable
    ItemConfiguration getItemConfiguration(String id);

    boolean hasItemConfiguration(String id);

    void unregisterItemConfiguration(String id);

    void reloadItemConfiguration(String id, ItemConfiguration config);

    void reloadAllConfigurations(ConfigurationSection configSection);

    Map<String, ItemConfiguration> getAllConfigurations();

    void clear();

    int size();
}
