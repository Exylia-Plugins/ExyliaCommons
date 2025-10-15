package net.exylia.commons.selection.events;

import lombok.Getter;
import net.exylia.commons.selection.model.Selection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class SelectionCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Selection selection;
    private final Location pos1;
    private final Location pos2;

    public SelectionCompleteEvent(Player player, Selection selection, Location pos1, Location pos2) {
        this.player = player;
        this.selection = selection;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
