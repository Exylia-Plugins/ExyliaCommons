package net.exylia.commons.ui.menus;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "Confirm Action";
        private String message = "";
        private Consumer<Player> onConfirm;
        private Consumer<Player> onCancel;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder onConfirm(Consumer<Player> callback) {
            this.onConfirm = callback;
            return this;
        }

        public Builder onCancel(Consumer<Player> callback) {
            this.onCancel = callback;
            return this;
        }

        public ConfirmationMenu build() {
            return new ConfirmationMenu(title, message, onConfirm, onCancel);
        }
    }

    private void setupItems() {
         
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

        if (message != null && !message.isEmpty()) {
            MenuItem messageItem = new SimpleItemBuilder(Material.PAPER)
                    .name("&e⚠ Confirmation")
                    .lore(message.split("\\|"))  
                    .build();

            setItem(13, messageItem);  
        }

        MenuItem filler = new SimpleItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAttributes()
                .build();

        setGlobalFiller(filler);
        setItem(11, confirmButton);  
        setItem(15, cancelButton);   
    }
}
