package net.exylia.commons.ui.items.provider;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderProvider implements CustomItemProvider {

    private static final String PROVIDER_NAME = "ItemsAdder";
    private boolean available = false;

    public ItemsAdderProvider() {
        this.available = isPluginLoaded();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    @Nullable
    public ItemStack getCustomItem(String identifier) {
        if (!available) {
            DebugUtils.logInternalDebug("ItemsAdder provider not available");
            return null;
        }

        try {
            dev.lone.itemsadder.api.CustomStack stack = dev.lone.itemsadder.api.CustomStack.getInstance(identifier);
            if (stack != null) {
                DebugUtils.logInternalDebug("ItemsAdder: Retrieved custom item " + identifier);
                return stack.getItemStack();
            }
            DebugUtils.logInternalDebug("ItemsAdder: Custom item " + identifier + " not found");
            return null;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("ItemsAdder: Error retrieving item " + identifier + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isCustomItem(ItemStack itemStack) {
        if (!available || itemStack == null) return false;

        try {
            boolean result = dev.lone.itemsadder.api.CustomStack.byItemStack(itemStack) != null;
            if (result) {
                DebugUtils.logInternalDebug("ItemsAdder: Detected custom item");
            }
            return result;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("ItemsAdder: Error checking custom item: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public String getCustomItemIdentifier(ItemStack itemStack) {
        if (!available || itemStack == null) return null;

        try {
            dev.lone.itemsadder.api.CustomStack stack = dev.lone.itemsadder.api.CustomStack.byItemStack(itemStack);
            if (stack != null) {
                String id = stack.getNamespacedID();
                DebugUtils.logInternalDebug("ItemsAdder: Got identifier: " + id);
                return id;
            }
            return null;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("ItemsAdder: Error getting identifier: " + e.getMessage());
            return null;
        }
    }

    private boolean isPluginLoaded() {
        return Bukkit.getPluginManager().getPlugin(PROVIDER_NAME) != null;
    }
}
