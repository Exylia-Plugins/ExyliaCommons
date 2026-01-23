package net.exylia.commons.region.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.model.Region;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Deprecated
public class RegionCreateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Region region;
    @Setter
    private boolean cancelled = false;

    public RegionCreateEvent(Region region) {
        this.region = region;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
