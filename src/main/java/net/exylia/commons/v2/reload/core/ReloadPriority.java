package net.exylia.commons.v2.reload.core;

import lombok.Getter;

@Getter
public enum ReloadPriority {
    CRITICAL(0),
    HIGH(100),
    NORMAL(200),
    LOW(300),
    CLEANUP(1000);

    private final int order;

    ReloadPriority(int order) {
        this.order = order;
    }
}
