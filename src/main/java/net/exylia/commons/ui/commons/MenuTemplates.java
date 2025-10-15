package net.exylia.commons.ui.commons;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.ConfirmationMenu;
import net.exylia.commons.ui.menus.SelectionMenu;
import net.exylia.commons.ui.menus.PaginationMenu;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuTemplates {

    public static ConfirmationMenu confirmation(String title, String message,
                                                Consumer<Player> onConfirm,
                                                Consumer<Player> onCancel) {
        return new ConfirmationMenu(title, message, onConfirm, onCancel);
    }

    public static SelectionMenu selection(String title, List<String> options,
                                          BiConsumer<Player, Integer> onSelect) {
        return new SelectionMenu(title, options, onSelect);
    }

    public static PaginationMenu pagination(String title, int rows, int... itemSlots) {
        return new PaginationMenu(title, rows, itemSlots);
    }

    public static Menu bordered(String title, int rows, Material borderMaterial) {
        Menu menu = new Menu(title, rows);

        MenuItem border = new SimpleItemBuilder(borderMaterial)
                .name(" ")
                .hideAttributes()
                .build();

        menu.setBorderFiller(border);
        return menu;
    }

    public static Menu filled(String title, int rows, Material fillerMaterial) {
        Menu menu = new Menu(title, rows);

        MenuItem filler = new SimpleItemBuilder(fillerMaterial)
                .name(" ")
                .hideAttributes()
                .build();

        menu.setGlobalFiller(filler);
        return menu;
    }
}
