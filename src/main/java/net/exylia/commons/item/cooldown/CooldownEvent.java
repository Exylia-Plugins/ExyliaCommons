package net.exylia.commons.item.cooldown;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Evento que se dispara cuando ocurren cambios en los cooldowns
 * Actualizado para usar double en lugar de int
 */
@Getter
public class CooldownEvent {

    private final CooldownEventType type;
    private final UUID playerId;
    private final String itemId;
    private final double seconds;
    private final long timestamp;

    public CooldownEvent(CooldownEventType type, UUID playerId, String itemId, double seconds) {
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