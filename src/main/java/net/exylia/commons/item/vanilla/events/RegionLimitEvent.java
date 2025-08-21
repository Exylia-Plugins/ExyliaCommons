package net.exylia.commons.item.vanilla.events;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@Getter
public class RegionLimitEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final RegionLimitEventType eventType;
    private final UUID playerId;
    private final String regionName;
    private final Material material;
    private final int currentUsage;
    private final int maxUsage;

    public RegionLimitEvent(RegionLimitEventType eventType, UUID playerId, String regionName, Material material, int currentUsage, int maxUsage) {
        super(true);
        this.eventType = eventType;
        this.playerId = playerId;
        this.regionName = regionName;
        this.material = material;
        this.currentUsage = currentUsage;
        this.maxUsage = maxUsage;
    }

    public Player getPlayer() {
        return org.bukkit.Bukkit.getPlayer(playerId);
    }

    public boolean isPlayerOnline() {
        return getPlayer() != null;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public String toString() {
        return "RegionLimitEvent{" +
                "eventType=" + eventType +
                ", playerId=" + playerId +
                ", regionName='" + regionName + '\'' +
                ", material=" + material +
                ", currentUsage=" + currentUsage +
                ", maxUsage=" + maxUsage +
                '}';
    }
}