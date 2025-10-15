package net.exylia.commons.item.vanilla;

import lombok.Getter;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

@Getter
public class VanillaItemConfig {

    private final Material material;
    private final double cooldownSeconds;
    private final VanillaTriggerType triggerType;
    private final String displayName;
    private final boolean hasDisplayName;
    private final Integer maxUsesPerRegion;
    private final Map<String, VanillaRegionConfig> regionConfigs;
    private final Integer maxUsesPerWorld;
    private final Map<String, VanillaWorldConfig> worldConfigs;

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType) {
        this(material, cooldownSeconds, triggerType, null, null, null, new HashMap<>(), new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName) {
        this(material, cooldownSeconds, triggerType, displayName, null, null, new HashMap<>(), new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion) {
        this(material, cooldownSeconds, triggerType, displayName, maxUsesPerRegion, null, new HashMap<>(), new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion, Map<String, VanillaRegionConfig> regionConfigs) {
        this(material, cooldownSeconds, triggerType, displayName, maxUsesPerRegion, null, regionConfigs, new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion, Integer maxUsesPerWorld, Map<String, VanillaRegionConfig> regionConfigs, Map<String, VanillaWorldConfig> worldConfigs) {
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.triggerType = triggerType != null ? triggerType : VanillaTriggerType.AUTO_DETECT;
        this.displayName = displayName;
        this.hasDisplayName = displayName != null && !displayName.trim().isEmpty();
        this.maxUsesPerRegion = maxUsesPerRegion;
        this.regionConfigs = regionConfigs != null ? regionConfigs : new HashMap<>();
        this.maxUsesPerWorld = maxUsesPerWorld;
        this.worldConfigs = worldConfigs != null ? worldConfigs : new HashMap<>();
    }

    public VanillaItemConfig(Material material, double cooldownSeconds) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null, null, null, new HashMap<>(), new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, String displayName) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName, null, null, new HashMap<>(), new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, Integer maxUsesPerRegion) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null, maxUsesPerRegion, null, new HashMap<>(), new HashMap<>());
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }

    public boolean hasDisplayName() {
        return hasDisplayName;
    }

    public boolean hasRegionLimit() {
        return maxUsesPerRegion != null && maxUsesPerRegion > 0;
    }

    public boolean hasRegionConfigs() {
        return !regionConfigs.isEmpty();
    }

    public boolean hasWorldLimit() {
        return maxUsesPerWorld != null && maxUsesPerWorld > 0;
    }

    public boolean hasWorldConfigs() {
        return !worldConfigs.isEmpty();
    }

    public double getCooldownForRegion(String regionName) {
        VanillaRegionConfig regionConfig = regionConfigs.get(regionName);
        return regionConfig != null ? regionConfig.getCooldown() : cooldownSeconds;
    }

    public int getMaxUsesForRegion(String regionName) {
        VanillaRegionConfig regionConfig = regionConfigs.get(regionName);
        if (regionConfig != null && regionConfig.getMaxUses() != null) {
            return regionConfig.getMaxUses();
        }
        return maxUsesPerRegion != null ? maxUsesPerRegion : -1;
    }
    
    public boolean isBlockedInRegion(String regionName) {
        VanillaRegionConfig regionConfig = regionConfigs.get(regionName);
        return regionConfig != null && regionConfig.isBlocked();
    }

    public double getCooldownForWorld(String worldName) {
        VanillaWorldConfig worldConfig = worldConfigs.get(worldName);
        return worldConfig != null ? worldConfig.getCooldown() : cooldownSeconds;
    }

    public int getMaxUsesForWorld(String worldName) {
        VanillaWorldConfig worldConfig = worldConfigs.get(worldName);
        if (worldConfig != null && worldConfig.getMaxUses() != null) {
            return worldConfig.getMaxUses();
        }
        return maxUsesPerWorld != null ? maxUsesPerWorld : -1;
    }
    
    public boolean isBlockedInWorld(String worldName) {
        VanillaWorldConfig worldConfig = worldConfigs.get(worldName);
        return worldConfig != null && worldConfig.isBlocked();
    }

    public String getEffectiveDisplayName() {
        if (hasDisplayName()) {
            return displayName;
        }
        return getDefaultDisplayName();
    }

    public String getDefaultDisplayName() {
        return formatMaterialName(material.name());
    }

    private String formatMaterialName(String materialName) {
        return materialName.toLowerCase()
                .replace("_", " ");
    }

    @Override
    public String toString() {
        return "VanillaItemConfig{" +
                "material=" + material +
                ", cooldownSeconds=" + cooldownSeconds +
                ", triggerType=" + triggerType +
                ", displayName='" + displayName + '\'' +
                ", hasDisplayName=" + hasDisplayName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VanillaItemConfig that = (VanillaItemConfig) o;
        return material == that.material;
    }

    @Override
    public int hashCode() {
        return material.hashCode();
    }
}
