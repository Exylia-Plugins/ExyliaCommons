package net.exylia.commons.region.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.model.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class RegionExitEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Region region;
    @Setter
    private boolean cancelled = false;

    public RegionExitEvent(Player player, Region region) {
        this.player = player;
        this.region = region;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
