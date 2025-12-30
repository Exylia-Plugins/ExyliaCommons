package net.exylia.commons.v2.ui.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.ui.exception.MenuException;
import net.exylia.commons.v2.ui.menu.*;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.model.MenuType;
import org.bukkit.entity.Player;

public class MenuFactory {

    public MenuBase create(Player player, MenuData menuData) {
        MenuType type = menuData.getType();

        DebugAPI.logLibDebug(DebugCategory.UI, "Creating menu of type " + type + " for player " + player.getName());

        MenuBase menu = switch (type) {
            case SIMPLE -> new SimpleMenu(player, menuData);
            case PAGINATION -> new PaginationMenu(player, menuData);
            case MULTI_PAGINATION -> new MultiPaginationMenu(player, menuData);
            case FULL_INVENTORY -> new FullInventoryMenu(player, menuData);
            case PAGINATION_FULL -> new PaginationFullMenu(player, menuData);
            case MULTI_PAGINATION_FULL -> new MultiPaginationFullMenu(player, menuData);
            default -> throw new MenuException("Unknown menu type: " + type);
        };

        DebugAPI.logLibDebug(DebugCategory.UI, "Menu created successfully: " + menu.getMenuId() + " (type: " + type + ")");

        return menu;
    }
}
