package net.exylia.commons.v2.clientapi.waypoint.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class WaypointDefinition {

    private final String name;
    private final int x;
    private final int y;
    private final int z;

    @Builder.Default
    private final String worldName = "world";

    private final UUID worldId;

    @Builder.Default
    private final WaypointColor color = WaypointColor.of(255, 255, 255);

    @Builder.Default
    private final WaypointDuration duration = WaypointDuration.permanent();

    @Builder.Default
    private final boolean preventRemoval = false;

    @Builder.Default
    private final boolean hidden = false;

    @Builder.Default
    private final boolean large = false;
}
