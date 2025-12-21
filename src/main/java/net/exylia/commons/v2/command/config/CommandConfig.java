package net.exylia.commons.v2.command.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommandConfig {
    private final String command;

    @Builder.Default
    private final String type = "player";

    @Builder.Default
    private final long delay = 0;

    private final String permission;
    private final String condition;

    @Builder.Default
    private final boolean async = true;
}
