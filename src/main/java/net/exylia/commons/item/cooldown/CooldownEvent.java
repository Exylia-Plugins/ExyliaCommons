package net.exylia.commons.item.cooldown;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Evento que se dispara cuando ocurren cambios en los cooldowns
 * Actualizado para usar double en lugar de int
 */
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

    /**
     * Obtiene el tipo de evento de cooldown
     * @return Tipo de evento
     */
    public CooldownEventType getType() {
        return type;
    }

    /**
     * Obtiene el UUID del jugador
     * @return UUID del jugador
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Obtiene el jugador (puede ser null si no está online)
     * @return Jugador o null
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    /**
     * Obtiene el ID del item
     * @return ID del item
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Obtiene los segundos del cooldown (para eventos SET) - con decimales
     * @return Segundos del cooldown (double)
     */
    public double getSeconds() {
        return seconds;
    }

    /**
     * Obtiene el timestamp del evento
     * @return Timestamp en milisegundos
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Verifica si el jugador está online
     * @return true si está online
     */
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