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

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType) {
        this(material, cooldownSeconds, triggerType, null, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName) {
        this(material, cooldownSeconds, triggerType, displayName, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion) {
        this(material, cooldownSeconds, triggerType, displayName, maxUsesPerRegion, new HashMap<>());
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName, Integer maxUsesPerRegion, Map<String, VanillaRegionConfig> regionConfigs) {
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.triggerType = triggerType != null ? triggerType : VanillaTriggerType.AUTO_DETECT;
        this.displayName = displayName;
        this.hasDisplayName = displayName != null && !displayName.trim().isEmpty();
        this.maxUsesPerRegion = maxUsesPerRegion;
        this.regionConfigs = regionConfigs != null ? regionConfigs : new HashMap<>();
    }

    public VanillaItemConfig(Material material, double cooldownSeconds) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, String displayName) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, Integer maxUsesPerRegion) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null, maxUsesPerRegion);
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

    /**
     * Obtiene el display name efectivo del item
     * Si tiene display-name personalizado, lo usa; sino usa el nombre del material
     */
    public String getEffectiveDisplayName() {
        if (hasDisplayName()) {
            return displayName;
        }
        return getDefaultDisplayName();
    }

    /**
     * Obtiene el nombre por defecto basado en el material
     */
    public String getDefaultDisplayName() {
        return formatMaterialName(material.name());
    }

    /**
     * Formatea el nombre del material para ser más legible
     */
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