package net.exylia.commons.v2.cooldown.model;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Getter
@Builder
public class ItemCooldownDefinition {

    private final String id;
    private final Material material;
    private final String displayName;
    private final long durationMs;

    @Builder.Default
    private final CooldownTrigger trigger = CooldownTrigger.USE;

    @Builder.Default
    private final Map<String, Long> regionDurationsMs = Collections.emptyMap();

    @Builder.Default
    private final Map<String, Integer> regionMaxUses = Collections.emptyMap();

    @Builder.Default
    private final Map<String, Long> worldDurationsMs = Collections.emptyMap();

    public boolean hasRegionDuration(String regionId) {
        return regionDurationsMs.containsKey(regionId);
    }

    public long getRegionDurationMs(String regionId) {
        return regionDurationsMs.getOrDefault(regionId, durationMs);
    }

    public boolean hasRegionMaxUses(String regionId) {
        return regionMaxUses.containsKey(regionId);
    }

    public int getRegionMaxUses(String regionId) {
        return regionMaxUses.getOrDefault(regionId, 0);
    }

    public boolean hasWorldDuration(String worldName) {
        return worldDurationsMs.containsKey(worldName);
    }

    public long getWorldDurationMs(String worldName) {
        return worldDurationsMs.getOrDefault(worldName, durationMs);
    }

    public boolean hasAnyRegionDuration() {
        return !regionDurationsMs.isEmpty();
    }

    public boolean hasAnyRegionMaxUses() {
        return !regionMaxUses.isEmpty();
    }

    public boolean hasAnyWorldDuration() {
        return !worldDurationsMs.isEmpty();
    }
}
