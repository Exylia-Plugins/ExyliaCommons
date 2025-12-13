package net.exylia.commons.v2.action.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionPriority {
    LOWEST(0),
    LOW(25),
    NORMAL(50),
    HIGH(75),
    HIGHEST(100);

    private final int value;
}
