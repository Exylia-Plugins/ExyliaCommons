package net.exylia.commons.ui.factory;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.menus.*;
import net.exylia.commons.ui.builders.EditableMenuBuilder;
import net.exylia.commons.ui.commons.MenuTemplates;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuFactory {

    public static Menu basic(String title, int rows) {
        return new Menu(title, rows);
    }

    public static Menu basic(String title, int rows, ExyliaContext context) {
        return new Menu(title, rows, context);
    }

    public static ConfirmationMenu confirmation(String title, String message,
                                                Consumer<Player> onConfirm,
                                                Consumer<Player> onCancel) {
        return MenuTemplates.confirmation(title, message, onConfirm, onCancel);
    }

    public static SelectionMenu selection(String title, List<String> options,
                                          BiConsumer<Player, Integer> onSelect) {
        return MenuTemplates.selection(title, options, onSelect);
    }

    public static PaginationMenu pagination(String title, int rows, int... itemSlots) {
        return MenuTemplates.pagination(title, rows, itemSlots);
    }

    public static EditableMenu editable(String title, int rows) {
        return new EditableMenu(title, rows, null);
    }

    public static EditableMenu editable(String title, int rows, ExyliaContext context) {
        return new EditableMenu(title, rows, context);
    }

    public static EditableMenuBuilder editableBuilder(String title, int rows) {
        return new EditableMenuBuilder(title, rows);
    }

    public static EditableMenuBuilder editableBuilder(String title, int rows, ExyliaContext context) {
        return new EditableMenuBuilder(title, rows, context);
    }

    public static Menu bordered(String title, int rows, Material borderMaterial) {
        return MenuTemplates.bordered(title, rows, borderMaterial);
    }

    public static Menu filled(String title, int rows, Material fillerMaterial) {
        return MenuTemplates.filled(title, rows, fillerMaterial);
    }

    public static MultiPaginationMenu multiPagination(String title, int rows) {
        return new MultiPaginationMenu(title, rows);
    }

    public static MultiPaginationMenu multiPagination(String title, int rows, ExyliaContext context) {
        return new MultiPaginationMenu(title, rows, context);
    }
}
