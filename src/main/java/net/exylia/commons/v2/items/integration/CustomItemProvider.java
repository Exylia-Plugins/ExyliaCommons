package net.exylia.commons.v2.items.integration;

import org.bukkit.inventory.ItemStack;

public interface CustomItemProvider {
    boolean supports(String namespace);
    ItemStack getItem(String id);
}
