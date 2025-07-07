package net.exylia.commons.region.events;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento llamado cuando un jugador intenta salir de una región
 * Este evento se dispara antes que RegionExitEvent y puede ser cancelado
 */
@Getter
public class RegionPreExitEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Region region;
    private final Location from;
    private final Location to;
    @Setter
    private boolean cancelled = false;
    @Setter
    private String cancelMessage;

    public RegionPreExitEvent(Player player, Region region, Location from, Location to) {
        this.player = player;
        this.region = region;
        this.from = from;
        this.to = to;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}