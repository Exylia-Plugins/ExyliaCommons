package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class Inventory_1_18_Adapter implements InventoryAdapter {

    @Override
    public Inventory createInventory(int size, Component title) {
         
        return Bukkit.createInventory(null, size, title);
    }
}
