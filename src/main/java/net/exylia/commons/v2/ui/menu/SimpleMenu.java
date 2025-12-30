package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public class SimpleMenu extends MenuBase {

    public SimpleMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menuData.getSize(), ColorAPI.parse(menuData.getTitle()));
    }

    @Override
    protected void populateItems() {
        applyFillers();
        applyStaticItems();
    }

    @Override
    protected void handleClickInternal(int slot, ClickType clickType) {
    }
}
