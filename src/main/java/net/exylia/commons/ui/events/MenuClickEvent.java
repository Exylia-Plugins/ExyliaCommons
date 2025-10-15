package net.exylia.commons.ui.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.function.Consumer;

@Getter
public class MenuClickEvent {

    private final Player player;
    private final Menu menu;
    private final MenuItem item;
    private final int slot;
    private final ClickType clickType;
    @Setter
    private boolean cancelled = false;

    public MenuClickEvent(Player player, Menu menu, MenuItem item, int slot, ClickType clickType) {
        this.player = player;
        this.menu = menu;
        this.item = item;
        this.slot = slot;
        this.clickType = clickType;
    }
     
    public boolean isLeftClick() {
        return clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;
    }

    public boolean isRightClick() {
        return clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isShiftClick() {
        return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isMiddleClick() {
        return clickType == ClickType.MIDDLE;
    }

    public void updateItem(MenuItem newItem) {
        menu.setItem(slot, newItem);
    }

    public void closeMenu() {
        menu.close();
    }

    public void openParentMenu() {
        if (menu.getParentMenu() != null) {
            menu.getParentMenu().open(player, menu.getContext());
        } else {
            menu.close();
        }
    }
}
