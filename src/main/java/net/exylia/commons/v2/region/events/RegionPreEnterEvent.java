package net.exylia.commons.v2.region.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@Getter
@Setter
public class RegionPreEnterEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Region region;
    private final Location from;
    private final Location to;
    private boolean cancelled = false;
    private String cancelMessage;

    public RegionPreEnterEvent(Player player, Region region, Location from, Location to) {
        super(player);
        this.region = region;
        this.from = from;
        this.to = to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
