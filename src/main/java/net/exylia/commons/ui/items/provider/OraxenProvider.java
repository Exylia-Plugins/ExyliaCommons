package net.exylia.commons.ui.items.provider;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class OraxenProvider implements CustomItemProvider {

    private static final String PROVIDER_NAME = "Oraxen";
    private static final String ORAXEN_ITEMS_CLASS = "io.th0rgal.oraxen.api.OraxenItems";
    private boolean available = false;
    private Object oraxenItems;
    private Method getItemByIdMethod;
    private Method getIdByItemMethod;

    public OraxenProvider() {
        this.available = initializeReflection();
    }

    private boolean initializeReflection() {
        if (!isPluginLoaded()) {
            DebugUtils.logInternalDebug("Oraxen plugin not loaded");
            return false;
        }

        try {
            Class<?> oraxenItemsClass = Class.forName(ORAXEN_ITEMS_CLASS);
            Field instanceField = oraxenItemsClass.getField("INSTANCE");
            this.oraxenItems = instanceField.get(null);

            this.getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
            this.getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);

            DebugUtils.logInternalSuccess("Oraxen provider initialized via reflection");
            return true;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Failed to initialize Oraxen provider: " + e.getMessage());
            return false;
        }
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
            DebugUtils.logInternalDebug("Oraxen provider not available");
            return null;
        }

        try {
            ItemStack item = (ItemStack) getItemByIdMethod.invoke(oraxenItems, identifier);
            if (item != null) {
                DebugUtils.logInternalDebug("Oraxen: Retrieved custom item " + identifier);
                return item.clone();
            }
            DebugUtils.logInternalDebug("Oraxen: Custom item " + identifier + " not found");
            return null;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Oraxen: Error retrieving item " + identifier + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isCustomItem(ItemStack itemStack) {
        if (!available || itemStack == null) return false;

        try {
            boolean result = getIdByItemMethod.invoke(oraxenItems, itemStack) != null;
            if (result) {
                DebugUtils.logInternalDebug("Oraxen: Detected custom item");
            }
            return result;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Oraxen: Error checking custom item: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public String getCustomItemIdentifier(ItemStack itemStack) {
        if (!available || itemStack == null) return null;

        try {
            String id = (String) getIdByItemMethod.invoke(oraxenItems, itemStack);
            if (id != null) {
                DebugUtils.logInternalDebug("Oraxen: Got identifier: " + id);
            }
            return id;
        } catch (Exception e) {
            DebugUtils.logInternalWarn("Oraxen: Error getting identifier: " + e.getMessage());
            return null;
        }
    }

    private boolean isPluginLoaded() {
        return Bukkit.getPluginManager().getPlugin(PROVIDER_NAME) != null;
    }
}
