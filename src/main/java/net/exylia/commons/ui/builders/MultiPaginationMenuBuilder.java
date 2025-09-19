// ==================== MULTI PAGINATION MENU BUILDER ====================

package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.MultiPaginationMenu;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builder for creating MultiPaginationMenu instances
 */
public class MultiPaginationMenuBuilder {

    private final MultiPaginationMenu menu;

    public MultiPaginationMenuBuilder(String title, int rows) {
        this.menu = new MultiPaginationMenu(title, rows);
    }

    public MultiPaginationMenuBuilder(String title, int rows, ExyliaContext context) {
        this.menu = new MultiPaginationMenu(title, rows, context);
    }

    /**
     * Adds a pagination section
     * @param name The section name
     * @param slots The slots for items
     * @return Section builder for chaining
     */
    public SectionBuilder addSection(String name, int... slots) {
        MultiPaginationMenu.PaginationSection section = menu.addSection(name, slots);
        return new SectionBuilder(this, section);
    }

    /**
     * Sets the section update handler
     * @param handler The update handler
     * @return This builder for chaining
     */
    public MultiPaginationMenuBuilder onSectionUpdate(BiConsumer<String, Integer> handler) {
        menu.setOnSectionUpdate(handler);
        return this;
    }

    /**
     * Sets the global filler
     * @param filler The filler item
     * @return This builder for chaining
     */
    public MultiPaginationMenuBuilder globalFiller(MenuItem filler) {
        menu.setGlobalFiller(filler);
        return this;
    }

    /**
     * Sets the border filler
     * @param filler The border filler item
     * @return This builder for chaining
     */
    public MultiPaginationMenuBuilder borderFiller(MenuItem filler) {
        menu.setBorderFiller(filler);
        return this;
    }

    /**
     * Sets the close handler
     * @param handler The close handler
     * @return This builder for chaining
     */
    public MultiPaginationMenuBuilder onClose(Consumer<Player> handler) {
        menu.setCloseHandler(handler);
        return this;
    }

    /**
     * Enables dynamic updates
     * @param plugin The plugin instance
     * @param interval The update interval
     * @return This builder for chaining
     */
    public MultiPaginationMenuBuilder dynamicUpdates(org.bukkit.plugin.java.JavaPlugin plugin, long interval) {
        menu.enableDynamicUpdates(plugin, interval);
        return this;
    }

    /**
     * Builds the menu
     * @return The built menu
     */
    public MultiPaginationMenu build() {
        return menu;
    }

    // ==================== SECTION BUILDER ====================

    /**
     * Builder for configuring individual sections
     */
    public static class SectionBuilder {
        private final MultiPaginationMenuBuilder parent;
        private final MultiPaginationMenu.PaginationSection section;

        public SectionBuilder(MultiPaginationMenuBuilder parent, MultiPaginationMenu.PaginationSection section) {
            this.parent = parent;
            this.section = section;
        }

        /**
         * Adds items to this section
         * @param items The items to add
         * @return This section builder for chaining
         */
        public SectionBuilder items(Collection<MenuItem> items) {
            section.addItems(items);
            return this;
        }

        /**
         * Adds a single item to this section
         * @param item The item to add
         * @return This section builder for chaining
         */
        public SectionBuilder addItem(MenuItem item) {
            section.addItem(item);
            return this;
        }

        /**
         * Sets the previous page button
         * @param button The button item
         * @param slot The button slot
         * @return This section builder for chaining
         */
        public SectionBuilder previousButton(MenuItem button, int slot) {
            section.setPreviousButton(button, slot);
            return this;
        }

        /**
         * Sets the next page button
         * @param button The button item
         * @param slot The button slot
         * @return This section builder for chaining
         */
        public SectionBuilder nextButton(MenuItem button, int slot) {
            section.setNextButton(button, slot);
            return this;
        }

        /**
         * Sets the filler item for this section
         * @param filler The filler item
         * @return This section builder for chaining
         */
        public SectionBuilder filler(MenuItem filler) {
            section.setFillerItem(filler);
            return this;
        }

        /**
         * Sets the selected item template
         * @param template The selected item template
         * @return This section builder for chaining
         */
        public SectionBuilder selectedTemplate(MenuItem template) {
            section.setSelectedItemTemplate(template);
            return this;
        }

        /**
         * Sets the item selection handler
         * @param handler The selection handler
         * @return This section builder for chaining
         */
        public SectionBuilder onItemSelect(BiFunction<net.exylia.commons.ui.events.MenuClickEvent, Integer, Boolean> handler) {
            section.setOnItemSelect(handler);
            return this;
        }

        /**
         * Sets the page change handler
         * @param handler The page change handler
         * @return This section builder for chaining
         */
        public SectionBuilder onPageChange(Consumer<Integer> handler) {
            section.setOnPageChange(handler);
            return this;
        }

        /**
         * Returns to the main menu builder
         * @return The parent menu builder
         */
        public MultiPaginationMenuBuilder and() {
            return parent;
        }

        /**
         * Builds the menu (convenience method)
         * @return The built menu
         */
        public MultiPaginationMenu build() {
            return parent.build();
        }
    }
}