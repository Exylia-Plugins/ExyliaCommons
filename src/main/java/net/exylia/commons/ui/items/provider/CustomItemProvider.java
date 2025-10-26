package net.exylia.commons.ui.items.provider;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface CustomItemProvider {

    String getProviderName();

    boolean isAvailable();

    @Nullable
    ItemStack getCustomItem(String identifier);

    boolean isCustomItem(ItemStack itemStack);

    @Nullable
    String getCustomItemIdentifier(ItemStack itemStack);
}
