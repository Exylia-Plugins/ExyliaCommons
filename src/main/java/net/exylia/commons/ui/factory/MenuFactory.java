package net.exylia.commons.ui.factory;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.menus.*;
import net.exylia.commons.ui.commons.MenuTemplates;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Factory for creating common menu types
 */
public class MenuFactory {

    /**
     * Creates a basic menu
     * @param title The title
     * @param rows Number of rows
     * @return The menu
     */
    public static Menu basic(String title, int rows) {
        return new Menu(title, rows);
    }

    /**
     * Creates a basic menu with context
     * @param title The title
     * @param rows Number of rows
     * @param context The context
     * @return The menu
     */
    public static Menu basic(String title, int rows, ExyliaContext context) {
        return new Menu(title, rows, context);
    }

    /**
     * Creates a confirmation menu
     * @param title The title
     * @param message The confirmation message
     * @param onConfirm Confirm callback
     * @param onCancel Cancel callback
     * @return The confirmation menu
     */
    public static ConfirmationMenu confirmation(String title, String message,
                                                Consumer<Player> onConfirm,
                                                Consumer<Player> onCancel) {
        return MenuTemplates.confirmation(title, message, onConfirm, onCancel);
    }

    /**
     * Creates a selection menu
     * @param title The title
     * @param options The options
     * @param onSelect Selection callback
     * @return The selection menu
     */
    public static SelectionMenu selection(String title, List<String> options,
                                          BiConsumer<Player, Integer> onSelect) {
        return MenuTemplates.selection(title, options, onSelect);
    }

    /**
     * Creates a pagination menu
     * @param title The title
     * @param rows Number of rows
     * @param itemSlots Slots for items
     * @return The pagination menu
     */
    public static PaginationMenu pagination(String title, int rows, int... itemSlots) {
        return MenuTemplates.pagination(title, rows, itemSlots);
    }

    /**
     * Creates an editable menu
     * @param title The title
     * @param rows Number of rows
     * @return The editable menu
     */
    public static EditableMenu editable(String title, int rows) {
        return new EditableMenu(title, rows);
    }

    /**
     * Creates a bordered menu
     * @param title The title
     * @param rows Number of rows
     * @param borderMaterial Border material
     * @return The bordered menu
     */
    public static Menu bordered(String title, int rows, Material borderMaterial) {
        return MenuTemplates.bordered(title, rows, borderMaterial);
    }

    /**
     * Creates a filled menu
     * @param title The title
     * @param rows Number of rows
     * @param fillerMaterial Filler material
     * @return The filled menu
     */
    public static Menu filled(String title, int rows, Material fillerMaterial) {
        return MenuTemplates.filled(title, rows, fillerMaterial);
    }

    /**
     * Creates a multi-pagination menu
     * @param title The title
     * @param rows Number of rows
     * @return The multi-pagination menu
     */
    public static MultiPaginationMenu multiPagination(String title, int rows) {
        return new MultiPaginationMenu(title, rows);
    }

    /**
     * Creates a multi-pagination menu with context
     * @param title The title
     * @param rows Number of rows
     * @param context The context
     * @return The multi-pagination menu
     */
    public static MultiPaginationMenu multiPagination(String title, int rows, ExyliaContext context) {
        return new MultiPaginationMenu(title, rows, context);
    }
}
