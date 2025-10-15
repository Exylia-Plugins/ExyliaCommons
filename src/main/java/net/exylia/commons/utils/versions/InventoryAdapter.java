package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;

public interface InventoryAdapter {
     
    Inventory createInventory(int size, Component title);
}
