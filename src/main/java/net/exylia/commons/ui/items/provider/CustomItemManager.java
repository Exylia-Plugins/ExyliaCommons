package net.exylia.commons.ui.items.provider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomItemManager {

    private static final CustomItemManager instance = new CustomItemManager();

    private final List<CustomItemProvider> providers = new ArrayList<>();
    private boolean initialized = false;

    public static CustomItemManager getInstance() {
        return instance;
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    public void initialize() {
        if (initialized) {
            DebugUtils.logInternalDebug("CustomItemManager already initialized");
            return;
        }

        DebugUtils.logInternalInfo("Initializing CustomItemManager...");
        providers.clear();
        registerProvider(new ItemsAdderProvider());
        registerProvider(new OraxenProvider());
        initialized = true;
        DebugUtils.logInternalInfo("CustomItemManager initialized with " + providers.size() + " providers");
    }

    public void registerProvider(CustomItemProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            if (provider.isAvailable()) {
                DebugUtils.logInternalSuccess("Provider registered: " + provider.getProviderName());
            } else {
                DebugUtils.logInternalDebug("Provider " + provider.getProviderName() + " not available");
            }
        }
    }

    public void unregisterProvider(CustomItemProvider provider) {
        providers.remove(provider);
        DebugUtils.logInternalDebug("Provider unregistered: " + provider.getProviderName());
    }

    @Nullable
    public ItemStack getCustomItem(String identifier) {
        ensureInitialized();

        if (identifier == null || identifier.isEmpty()) {
            DebugUtils.logInternalWarn("CustomItem identifier is null or empty");
            return null;
        }

        DebugUtils.logInternalDebug("Searching for custom item: " + identifier);

        for (CustomItemProvider provider : providers) {
            if (!provider.isAvailable()) continue;

            ItemStack item = provider.getCustomItem(identifier);
            if (item != null) {
                DebugUtils.logInternalDebug("Found custom item '" + identifier + "' from provider: " + provider.getProviderName());
                return item.clone();
            }
        }

        DebugUtils.logInternalWarn("Custom item not found: " + identifier);
        return null;
    }

    public boolean isCustomItem(ItemStack itemStack) {
        ensureInitialized();

        if (itemStack == null) return false;

        for (CustomItemProvider provider : providers) {
            if (!provider.isAvailable()) continue;

            if (provider.isCustomItem(itemStack)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public String getCustomItemIdentifier(ItemStack itemStack) {
        ensureInitialized();

        if (itemStack == null) return null;

        for (CustomItemProvider provider : providers) {
            if (!provider.isAvailable()) continue;

            String identifier = provider.getCustomItemIdentifier(itemStack);
            if (identifier != null) {
                return identifier;
            }
        }

        return null;
    }

    @Nullable
    public String getProviderNameForItem(ItemStack itemStack) {
        ensureInitialized();

        if (itemStack == null) return null;

        for (CustomItemProvider provider : providers) {
            if (!provider.isAvailable()) continue;

            if (provider.isCustomItem(itemStack)) {
                return provider.getProviderName();
            }
        }

        return null;
    }

    public List<String> getAvailableProviders() {
        ensureInitialized();

        List<String> availableProviders = new ArrayList<>();
        for (CustomItemProvider provider : providers) {
            if (provider.isAvailable()) {
                availableProviders.add(provider.getProviderName());
            }
        }
        return availableProviders;
    }
}
