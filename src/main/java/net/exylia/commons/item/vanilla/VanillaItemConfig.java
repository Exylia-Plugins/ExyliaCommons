package net.exylia.commons.item.vanilla;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Configuración de cooldown para un item vanilla
 */
@Getter
public class VanillaItemConfig {

    private final Material material;
    private final double cooldownSeconds;
    private final VanillaTriggerType triggerType;

    public VanillaItemConfig(Material material, double cooldownSeconds, VanillaTriggerType triggerType) {
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.triggerType = triggerType != null ? triggerType : VanillaTriggerType.AUTO_DETECT;
    }

    public VanillaItemConfig(Material material, double cooldownSeconds) {
        this(material, cooldownSeconds, VanillaTriggerType.AUTO_DETECT);
    }

    /**
     * Verifica si el cooldown está activo
     */
    public boolean hasCooldown() {
        return cooldownSeconds > 0.0;
    }

    @Override
    public String toString() {
        return "VanillaItemConfig{" +
                "material=" + material +
                ", cooldownSeconds=" + cooldownSeconds +
                ", triggerType=" + triggerType +
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