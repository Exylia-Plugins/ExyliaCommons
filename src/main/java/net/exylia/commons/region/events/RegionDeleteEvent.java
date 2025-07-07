package net.exylia.commons.region.events;

import lombok.Getter;
import net.exylia.commons.region.model.Region;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento llamado cuando se elimina una región
 */
@Getter
public class RegionDeleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Region region;

    public RegionDeleteEvent(Region region) {
        this.region = region;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}