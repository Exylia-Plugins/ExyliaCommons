package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.MultiPaginationMenu;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MultiPaginationMenuBuilder {

    private final MultiPaginationMenu menu;

    public MultiPaginationMenuBuilder(String title, int rows) {
        this.menu = new MultiPaginationMenu(title, rows);
    }

    public MultiPaginationMenuBuilder(String title, int rows, ExyliaContext context) {
        this.menu = new MultiPaginationMenu(title, rows, context);
    }

    public SectionBuilder addSection(String name, int... slots) {
        MultiPaginationMenu.PaginationSection section = menu.addSection(name, slots);
        return new SectionBuilder(this, section);
    }

    public MultiPaginationMenuBuilder onSectionUpdate(BiConsumer<String, Integer> handler) {
        menu.setOnSectionUpdate(handler);
        return this;
    }

    public MultiPaginationMenuBuilder globalFiller(MenuItem filler) {
        menu.setGlobalFiller(filler);
        return this;
    }

    public MultiPaginationMenuBuilder borderFiller(MenuItem filler) {
        menu.setBorderFiller(filler);
        return this;
    }

    public MultiPaginationMenuBuilder onClose(Consumer<Player> handler) {
        menu.setCloseHandler(handler);
        return this;
    }

    public MultiPaginationMenuBuilder dynamicUpdates(org.bukkit.plugin.java.JavaPlugin plugin, long interval) {
        menu.enableDynamicUpdates(plugin, interval);
        return this;
    }

    public MultiPaginationMenu build() {
        return menu;
    }

    public static class SectionBuilder {
        private final MultiPaginationMenuBuilder parent;
        private final MultiPaginationMenu.PaginationSection section;

        public SectionBuilder(MultiPaginationMenuBuilder parent, MultiPaginationMenu.PaginationSection section) {
            this.parent = parent;
            this.section = section;
        }

        public SectionBuilder items(Collection<MenuItem> items) {
            section.addItems(items);
            return this;
        }

        public SectionBuilder addItem(MenuItem item) {
            section.addItem(item);
            return this;
        }

        public SectionBuilder previousButton(MenuItem button, int slot) {
            section.setPreviousButton(button, slot);
            return this;
        }

        public SectionBuilder nextButton(MenuItem button, int slot) {
            section.setNextButton(button, slot);
            return this;
        }

        public SectionBuilder filler(MenuItem filler) {
            section.setFillerItem(filler);
            return this;
        }

        public SectionBuilder selectedTemplate(MenuItem template) {
            section.setSelectedItemTemplate(template);
            return this;
        }

        public SectionBuilder onItemSelect(BiFunction<net.exylia.commons.ui.events.MenuClickEvent, Integer, Boolean> handler) {
            section.setOnItemSelect(handler);
            return this;
        }

        public SectionBuilder onPageChange(Consumer<Integer> handler) {
            section.setOnPageChange(handler);
            return this;
        }

        public MultiPaginationMenuBuilder and() {
            return parent;
        }

        public MultiPaginationMenu build() {
            return parent.build();
        }
    }
}
