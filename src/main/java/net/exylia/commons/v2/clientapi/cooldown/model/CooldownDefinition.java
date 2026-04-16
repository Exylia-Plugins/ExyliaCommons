package net.exylia.commons.v2.clientapi.cooldown.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class CooldownDefinition {

    private final String name;
    private final Duration duration;

    @Builder.Default
    private final CooldownIcon icon = CooldownIcon.item("COMPASS");
}
