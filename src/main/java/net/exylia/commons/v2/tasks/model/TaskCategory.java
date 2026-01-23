package net.exylia.commons.v2.tasks.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskCategory {
    GENERAL("General", 4, Runtime.getRuntime().availableProcessors()),
    DATABASE("Database", 4, 4),
    IO("IO", 2, 8),
    COMPUTE("Compute", 2, Runtime.getRuntime().availableProcessors() * 2);

    private final String displayName;
    private final int corePoolSize;
    private final int maxPoolSize;
}
