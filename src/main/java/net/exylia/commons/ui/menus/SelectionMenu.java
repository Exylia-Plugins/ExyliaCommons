package net.exylia.commons.ui.menus;

import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

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
         
        int itemsPerRow = 7;  
        int neededRows = (int) Math.ceil((double) optionCount / itemsPerRow);
        return Math.max(3, Math.min(6, neededRows + 2));  
    }

    private void setupItems() {
         
        MenuItem filler = new SimpleItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAttributes()
                .build();
        setGlobalFiller(filler);

        int slot = 10;  
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

            slot++;
            if (slot % 9 == 8) {  
                slot += 2;  
            }

            if (slot >= size - 9) {  
                break;  
            }
        }
    }
}
