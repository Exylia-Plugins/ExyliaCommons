package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class Inventory_1_18_Adapter implements InventoryAdapter {

    @Override
    public Inventory createInventory(int size, Component title) {
        // En 1.17+ se puede usar directamente el componente
        return Bukkit.createInventory(null, size, title);
    }
}