package net.exylia.commons.v2.items.integration;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CustomItemManager {

    private static final CustomItemManager INSTANCE = new CustomItemManager();
    private final List<CustomItemProvider> providers = new ArrayList<>();

    private CustomItemManager() {}

    public static CustomItemManager getInstance() {
        return INSTANCE;
    }

    public void registerProvider(CustomItemProvider provider) {
        providers.add(provider);
    }

    public ItemStack getCustomItem(String id) {
        for (CustomItemProvider provider : providers) {
            String namespace = id.contains(":") ? id.substring(0, id.indexOf(':')) : "";
            if (provider.supports(namespace)) {
                ItemStack item = provider.getItem(id);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    public boolean hasCustomItem(String id) {
        return getCustomItem(id) != null;
    }
}
