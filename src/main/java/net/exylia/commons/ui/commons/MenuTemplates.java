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

/**
 * Common menu utilities and templates
 * Provides base templates without hardcoded styling
 */
public class MenuTemplates {

    /**
     * Creates a confirmation menu template
     * @param title The menu title
     * @param message The confirmation message
     * @param onConfirm Confirm callback
     * @param onCancel Cancel callback
     * @return The confirmation menu
     */
    public static ConfirmationMenu confirmation(String title, String message,
                                                Consumer<Player> onConfirm,
                                                Consumer<Player> onCancel) {
        return new ConfirmationMenu(title, message, onConfirm, onCancel);
    }

    /**
     * Creates a selection menu template
     * @param title The menu title
     * @param options The options to choose from
     * @param onSelect Selection callback
     * @return The selection menu
     */
    public static SelectionMenu selection(String title, List<String> options,
                                          BiConsumer<Player, Integer> onSelect) {
        return new SelectionMenu(title, options, onSelect);
    }

    /**
     * Creates a pagination menu template
     * @param title The menu title
     * @param rows Number of rows
     * @param itemSlots Slots for paginated items
     * @return The pagination menu
     */
    public static PaginationMenu pagination(String title, int rows, int... itemSlots) {
        return new PaginationMenu(title, rows, itemSlots);
    }

    /**
     * Creates a bordered menu template
     * @param title The menu title
     * @param rows Number of rows
     * @param borderMaterial Border material
     * @return The bordered menu
     */
    public static Menu bordered(String title, int rows, Material borderMaterial) {
        Menu menu = new Menu(title, rows);

        MenuItem border = new SimpleItemBuilder(borderMaterial)
                .name(" ")
                .hideAttributes()
                .build();

        menu.setBorderFiller(border);
        return menu;
    }

    /**
     * Creates a filled menu template
     * @param title The menu title
     * @param rows Number of rows
     * @param fillerMaterial Filler material
     * @return The filled menu
     */
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