package net.exylia.commons.v2.region.events;

import lombok.Getter;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@Getter
public class RegionExitEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Region region;

    public RegionExitEvent(Player player, Region region) {
        super(player);
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
