package net.exylia.commons.v2.tasks.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {
    LOW(Thread.MIN_PRIORITY),
    NORMAL(Thread.NORM_PRIORITY),
    HIGH(Thread.MAX_PRIORITY - 1),
    CRITICAL(Thread.MAX_PRIORITY);

    private final int threadPriority;
}
