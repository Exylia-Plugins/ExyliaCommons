package net.exylia.commons.v2.command.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Command {
    private final String id;
    private final String rawCommand;
    private final String processedCommand;
    private final CommandType type;
    private final CommandSource source;

    @Builder.Default
    private final boolean async = true;

    @Builder.Default
    private final long delay = 0L;

    private final String permission;
    private final String condition;
}
