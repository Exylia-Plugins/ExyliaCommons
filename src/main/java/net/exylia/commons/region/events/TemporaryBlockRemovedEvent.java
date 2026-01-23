package net.exylia.commons.region.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@Deprecated
public class TemporaryBlockRemovedEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Location location;
    private final Material material;
    private final String regionId;
    private final long placementTime;
    private boolean cancelled = false;

    public TemporaryBlockRemovedEvent(Location location, Material material, String regionId, long placementTime) {
        this.location = location.clone();
        this.material = material;
        this.regionId = regionId;
        this.placementTime = placementTime;
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
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public long getLifetimeMillis() {
        return System.currentTimeMillis() - placementTime;
    }

    public int getLifetimeSeconds() {
        return (int) (getLifetimeMillis() / 1000);
    }

    @Override
    public String toString() {
        return String.format("TemporaryBlockRemovedEvent{location=%s, material=%s, region=%s, lifetime=%ds}",
                location, material.name(), regionId, getLifetimeSeconds());
    }
}
