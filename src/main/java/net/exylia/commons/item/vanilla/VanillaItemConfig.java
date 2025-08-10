package net.exylia.commons.item.vanilla;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public class VanillaItemConfig {

    private final Material material;
    private final double cooldownSeconds;
    private final VanillaTriggerType triggerType;
    private final String displayName;
    private final boolean hasDisplayName;

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType) {
        this(material, cooldownSeconds, triggerType, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType, String displayName) {
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.triggerType = triggerType != null ? triggerType : VanillaTriggerType.AUTO_DETECT;
        this.displayName = displayName;
        this.hasDisplayName = displayName != null && !displayName.trim().isEmpty();
    }

    public VanillaItemConfig(Material material, double cooldownSeconds) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, null);
    }

    public VanillaItemConfig(Material material, double cooldownSeconds, String displayName) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT, displayName);
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }

    public boolean hasDisplayName() {
        return hasDisplayName;
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