package net.exylia.commons.v2.ui.event;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.ui.model.MenuItemV2;
import net.exylia.commons.v2.ui.model.MenuV2;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;

@Getter
@Setter
public class MenuClickEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final MenuV2 menu;
    private final MenuItemV2 item;
    private final int slot;
    private final ClickType clickType;
    private final boolean shiftClick;

    private boolean cancelled;

    public MenuClickEvent(Player player, MenuV2 menu, MenuItemV2 item, int slot, ClickType clickType, boolean shiftClick) {
        this.player = player;
        this.menu = menu;
        this.item = item;
        this.slot = slot;
        this.clickType = clickType;
        this.shiftClick = shiftClick;
        this.cancelled = false;
    }

    public boolean isLeftClick() {
        return clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;
    }

    public boolean isRightClick() {
        return clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT;
    }

    public boolean isMiddleClick() {
        return clickType == ClickType.MIDDLE;
    }

    public void updateItem(MenuItemV2 newItem) {
        if (menu != null && newItem != null) {
            menu.setItem(slot, newItem);
        }
    }

    public void closeMenu() {
        if (player != null) {
            player.closeInventory();
        }
    }

    public void openParentMenu() {
        if (menu != null && menu.getContext() != null && menu.getContext().getParentMenu() != null) {
            menu.getContext().getParentMenu().open(player, menu.getContext().getParentMenu().getContext());
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
