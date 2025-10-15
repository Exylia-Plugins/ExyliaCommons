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

@Getter
public class FlagState {
    private final UUID playerId;
    private final long createdAt;

    private final Set<RegionFlag> appliedFlags;

    @Setter
    private Region activeRegion;

    @Setter
    private GameMode previousGameMode;
    @Setter
    private boolean previousFlightState;
    @Setter
    private boolean previousFlyingState;

    private final ConcurrentHashMap<RegionFlag, Object> flagMetadata;

    @Setter
    private long lastUpdated;

    public FlagState(UUID playerId) {
        this.playerId = playerId;
        this.createdAt = System.currentTimeMillis();
        this.appliedFlags = EnumSet.noneOf(RegionFlag.class);
        this.flagMetadata = new ConcurrentHashMap<>();
        this.lastUpdated = createdAt;
    }

    public void addAppliedFlag(RegionFlag flag) {
        appliedFlags.add(flag);
        updateTimestamp();
    }

    public void removeAppliedFlag(RegionFlag flag) {
        appliedFlags.remove(flag);
        flagMetadata.remove(flag);
        updateTimestamp();
    }

    public boolean hasAppliedFlag(RegionFlag flag) {
        return appliedFlags.contains(flag);
    }

    public void clearAppliedFlags() {
        appliedFlags.clear();
        flagMetadata.clear();
        updateTimestamp();
    }

    public void setFlagMetadata(RegionFlag flag, Object metadata) {
        flagMetadata.put(flag, metadata);
        updateTimestamp();
    }

    @SuppressWarnings("unchecked")
    public <T> T getFlagMetadata(RegionFlag flag, Class<T> type) {
        Object value = flagMetadata.get(flag);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public boolean hasAnyAppliedFlags() {
        return !appliedFlags.isEmpty();
    }

    public boolean isValid(long maxAge) {
        return System.currentTimeMillis() - lastUpdated < maxAge;
    }

    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

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
