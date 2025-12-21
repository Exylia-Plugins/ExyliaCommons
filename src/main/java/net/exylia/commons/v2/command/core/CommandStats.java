package net.exylia.commons.v2.command.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommandStats {
    private final long cacheSize;
    private final boolean proxyEnabled;
}
