package net.exylia.commons.selection.events;

import lombok.Getter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Evento llamado cuando se usa una wand
 */
public class WandUseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player player;
    @Getter
    private final ItemStack wand;
    @Getter
    private final Location location;
    @Getter
    private final WandAction action;
    @Getter
    private final Selection selection;
    private boolean cancelled = false;

    public WandUseEvent(Player player, ItemStack wand, Location location, WandAction action, Selection selection) {
        this.player = player;
        this.wand = wand;
        this.location = location;
        this.action = action;
        this.selection = selection;
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    public enum WandAction {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
        SHIFT_LEFT_CLICK,
        SHIFT_RIGHT_CLICK
    }
}