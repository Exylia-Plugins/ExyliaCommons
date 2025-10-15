package net.exylia.commons.item.cooldown;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@Getter
public class CooldownEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CooldownEventType type;
    private final UUID playerId;
    private final String itemId;
    private final double seconds;
    private final long timestamp;

    public CooldownEvent(CooldownEventType type, UUID playerId, String itemId, double seconds) {
        super(true);  
        this.type = type;
        this.playerId = playerId;
        this.itemId = itemId;
        this.seconds = seconds;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
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
        return "CooldownEvent{" +
                "type=" + type +
                ", playerId=" + playerId +
                ", itemId='" + itemId + '\'' +
                ", seconds=" + seconds +
                ", timestamp=" + timestamp +
                '}';
    }
}
