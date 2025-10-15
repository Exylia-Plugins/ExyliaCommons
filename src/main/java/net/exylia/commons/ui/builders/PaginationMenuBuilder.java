package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.menus.PaginationMenu;
import net.exylia.commons.ui.items.MenuItem;

import java.util.Collection;

public class PaginationMenuBuilder {

    private final PaginationMenu menu;

    public PaginationMenuBuilder(String title, int rows, int... itemSlots) {
        this.menu = new PaginationMenu(title, rows, itemSlots);
    }

    public PaginationMenuBuilder items(Collection<MenuItem> items) {
        menu.setItems(items);
        return this;
    }

    public PaginationMenuBuilder previousButton(MenuItem button, int slot) {
        menu.setPreviousButton(button, slot);
        return this;
    }

    public PaginationMenuBuilder nextButton(MenuItem button, int slot) {
        menu.setNextButton(button, slot);
        return this;
    }

    public PaginationMenuBuilder itemSlotFiller(MenuItem filler) {
        menu.setItemSlotFiller(filler);
        return this;
    }

    public PaginationMenuBuilder globalFiller(MenuItem filler) {
        menu.setGlobalFiller(filler);
        return this;
    }

    public PaginationMenu build() {
        return menu;
    }
}
