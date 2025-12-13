package net.exylia.commons.v2.region.events;

import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class RegionCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Region region;

    public RegionCreateEvent(Region region) {
        super(true);
        this.region = region;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
