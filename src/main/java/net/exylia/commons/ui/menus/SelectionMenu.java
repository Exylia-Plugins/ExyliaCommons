

// ==================== SELECTION MENU ====================

package net.exylia.commons.ui.menus;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Menu for selecting from a list of options
 */
public class SelectionMenu extends Menu {

    private final List<String> options;
    private final BiConsumer<Player, Integer> onSelect;

    public SelectionMenu(String title, List<String> options, BiConsumer<Player, Integer> onSelect) {
        super(title, calculateRows(options.size()));
        this.options = options;
        this.onSelect = onSelect;

        setupItems();
    }

    private static int calculateRows(int optionCount) {
        // Calculate needed rows, leaving space for borders and navigation
        int itemsPerRow = 7; // Avoid edges
        int neededRows = (int) Math.ceil((double) optionCount / itemsPerRow);
        return Math.max(3, Math.min(6, neededRows + 2)); // +2 for top/bottom borders
    }

    private void setupItems() {
        // Filler
        MenuItem filler = new SimpleItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAttributes()
                .build();
        setGlobalFiller(filler);

        // Add options
        int slot = 10; // Start position (avoiding edges)
        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            String option = options.get(i);

            MenuItem optionItem = new SimpleItemBuilder(Material.PAPER)
                    .name("&f" + option)
                    .lore("&7Click to select this option")
                    .click(event -> {
                        if (onSelect != null) {
                            onSelect.accept(event.getPlayer(), index);
                        }
                        event.closeMenu();
                    })
                    .build();

            setItem(slot, optionItem);

            // Move to next valid slot
            slot++;
            if (slot % 9 == 8) { // If at right edge
                slot += 2; // Skip to next row, avoiding edges
            }

            if (slot >= size - 9) { // If approaching bottom row
                break; // Stop to avoid overflow
            }
        }
    }
}