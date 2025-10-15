package net.exylia.commons.item.vanilla;

import lombok.Getter;

@Getter
public class VanillaRegionConfig {
    
    private final double cooldown;
    private final Integer maxUses;
    
    public VanillaRegionConfig(double cooldown, Integer maxUses) {
        this.cooldown = cooldown;
        this.maxUses = maxUses;
    }
    
    public boolean hasMaxUses() {
        return maxUses != null && maxUses > 0;
    }
    
    public boolean isBlocked() {
        return maxUses != null && maxUses == 0;
    }
}
