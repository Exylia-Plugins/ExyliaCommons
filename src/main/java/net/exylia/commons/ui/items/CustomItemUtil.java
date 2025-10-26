package net.exylia.commons.ui.items;

import net.exylia.commons.ui.items.provider.CustomItemManager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CustomItemUtil {

    private CustomItemUtil() {
    }

    public static MenuItem createFromCustomItem(String customItemIdentifier) {
        ItemStack customItem = CustomItemManager.getInstance().getCustomItem(customItemIdentifier);
        if (customItem != null) {
            return new MenuItem(customItem);
        }
        return new MenuItem("STONE");
    }

    public static MenuItem createFromCustomItem(String customItemIdentifier, String fallbackMaterial) {
        ItemStack customItem = CustomItemManager.getInstance().getCustomItem(customItemIdentifier);
        if (customItem != null) {
            return new MenuItem(customItem);
        }
        return new MenuItem(fallbackMaterial);
    }

    public static boolean isCustomItem(ItemStack itemStack) {
        return CustomItemManager.getInstance().isCustomItem(itemStack);
    }

    @Nullable
    public static String getCustomItemIdentifier(ItemStack itemStack) {
        return CustomItemManager.getInstance().getCustomItemIdentifier(itemStack);
    }

    @Nullable
    public static String getProviderName(ItemStack itemStack) {
        return CustomItemManager.getInstance().getProviderNameForItem(itemStack);
    }

    public static java.util.List<String> getAvailableProviders() {
        return CustomItemManager.getInstance().getAvailableProviders();
    }

    public static void initializeProviders() {
        CustomItemManager.getInstance().initialize();
    }
}
