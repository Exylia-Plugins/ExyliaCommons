package net.exylia.commons.selection.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento llamado cuando se crea una nueva selección
 */
@Getter
public class SelectionCreateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Selection selection;
    @Setter
    private boolean cancelled = false;

    public SelectionCreateEvent(Player player, Selection selection) {
        this.player = player;
        this.selection = selection;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}