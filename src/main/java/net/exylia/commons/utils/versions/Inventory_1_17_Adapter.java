package net.exylia.commons.utils.versions;

import net.exylia.commons.utils.OldColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class Inventory_1_17_Adapter implements InventoryAdapter {

    @Override
    public Inventory createInventory(int size, Component title) {
         
        String miniMessageString = MiniMessage.miniMessage().serialize(title);
        return Bukkit.createInventory(null, size, OldColorUtils.parseOld(miniMessageString));
    }
}
