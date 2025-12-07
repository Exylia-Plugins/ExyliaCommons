package net.exylia.commons.v2.hologram.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@With
public class HologramConfig {
    @Builder.Default
    private final long updateInterval = 20L;

    @Builder.Default
    private final boolean autoUpdate = true;

    @Builder.Default
    private final boolean spawnOnChunkLoad = true;

    @Builder.Default
    private final boolean removeOnChunkUnload = true;

    public static HologramConfig defaultConfig() {
        return HologramConfig.builder().build();
    }

    public boolean shouldUpdate() {
        return autoUpdate && updateInterval > 0;
    }
}
