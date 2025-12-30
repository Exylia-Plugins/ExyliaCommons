package net.exylia.commons.v2.hologram.model;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;

import java.util.List;

@Getter
@Builder
public class HologramTemplate {
    @Builder.Default
    private final boolean enabled = true;

    private final List<String> lines;
    private final HologramProperties properties;
    private final HologramConfig config;
    private final boolean persistent;
    private final boolean perPlayer;
    private final VisibilityCondition visibilityCondition;
    private final double viewDistance;

    @Builder.Default
    private final double offsetX = 0.0;
    @Builder.Default
    private final double offsetY = 0.0;
    @Builder.Default
    private final double offsetZ = 0.0;
}
