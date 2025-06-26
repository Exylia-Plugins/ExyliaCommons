package net.exylia.commons.ui.menus;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Simple confirmation menu with accept/cancel options
 */
public class ConfirmationMenu extends Menu {

    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;
    private final String message;

    public ConfirmationMenu(String title, String message, Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        super(title, 3);
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        setupItems();
    }

    private void setupItems() {
        // Confirm button (green)
        MenuItem confirmButton = new SimpleItemBuilder(Material.GREEN_WOOL)
                .name("&a✓ Confirm")
                .lore("&7Click to confirm this action")
                .click(event -> {
                    event.closeMenu();
                    if (onConfirm != null) {
                        onConfirm.accept(event.getPlayer());
                    }
                })
                .build();

        // Cancel button (red)
        MenuItem cancelButton = new SimpleItemBuilder(Material.RED_WOOL)
                .name("&c✗ Cancel")
                .lore("&7Click to cancel this action")
                .click(event -> {
                    event.closeMenu();
                    if (onCancel != null) {
                        onCancel.accept(event.getPlayer());
                    }
                })
                .build();

        // Message item (if provided)
        if (message != null && !message.isEmpty()) {
            MenuItem messageItem = new SimpleItemBuilder(Material.PAPER)
                    .name("&e⚠ Confirmation")
                    .lore(message.split("\\|")) // Support multiple lines with |
                    .build();

            setItem(13, messageItem); // Center slot
        }

        // Filler
        MenuItem filler = new SimpleItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAttributes()
                .build();

        setGlobalFiller(filler);
        setItem(11, confirmButton); // Left side
        setItem(15, cancelButton);  // Right side
    }
}