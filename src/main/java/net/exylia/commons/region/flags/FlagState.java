package net.exylia.commons.region.flags;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import org.bukkit.GameMode;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa el estado actual de las flags aplicadas a un jugador
 */
@Getter
public class FlagState {
    private final UUID playerId;
    private final long createdAt;

    // Flags actualmente aplicadas al jugador
    private final Set<RegionFlag> appliedFlags;

    // Región activa donde está el jugador
    @Setter
    private Region activeRegion;

    // Estados anteriores para restauración
    @Setter
    private GameMode previousGameMode;
    @Setter
    private boolean previousFlightState;
    @Setter
    private boolean previousFlyingState;

    // Metadatos adicionales por flag
    private final ConcurrentHashMap<RegionFlag, Object> flagMetadata;

    // Timestamp de última actualización
    @Setter
    private long lastUpdated;

    public FlagState(UUID playerId) {
        this.playerId = playerId;
        this.createdAt = System.currentTimeMillis();
        this.appliedFlags = EnumSet.noneOf(RegionFlag.class);
        this.flagMetadata = new ConcurrentHashMap<>();
        this.lastUpdated = createdAt;
    }

    /**
     * Añade una flag como aplicada
     */
    public void addAppliedFlag(RegionFlag flag) {
        appliedFlags.add(flag);
        updateTimestamp();
    }

    /**
     * Remueve una flag aplicada
     */
    public void removeAppliedFlag(RegionFlag flag) {
        appliedFlags.remove(flag);
        flagMetadata.remove(flag);
        updateTimestamp();
    }

    /**
     * Verifica si una flag está aplicada
     */
    public boolean hasAppliedFlag(RegionFlag flag) {
        return appliedFlags.contains(flag);
    }

    /**
     * Limpia todas las flags aplicadas
     */
    public void clearAppliedFlags() {
        appliedFlags.clear();
        flagMetadata.clear();
        updateTimestamp();
    }

    /**
     * Establece metadata para una flag específica
     */
    public void setFlagMetadata(RegionFlag flag, Object metadata) {
        flagMetadata.put(flag, metadata);
        updateTimestamp();
    }

    /**
     * Obtiene metadata de una flag específica
     */
    @SuppressWarnings("unchecked")
    public <T> T getFlagMetadata(RegionFlag flag, Class<T> type) {
        Object value = flagMetadata.get(flag);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Verifica si el estado tiene alguna flag aplicada
     */
    public boolean hasAnyAppliedFlags() {
        return !appliedFlags.isEmpty();
    }

    /**
     * Verifica si el estado es válido (no muy antiguo)
     */
    public boolean isValid(long maxAge) {
        return System.currentTimeMillis() - lastUpdated < maxAge;
    }

    /**
     * Actualiza el timestamp
     */
    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Obtiene información resumida del estado
     */
    public String getSummary() {
        return "FlagState{" +
                "player=" + playerId +
                ", region=" + (activeRegion != null ? activeRegion.getId() : "none") +
                ", flags=" + appliedFlags.size() +
                ", lastUpdated=" + lastUpdated +
                "}";
    }

    @Override
    public String toString() {
        return getSummary();
    }
}