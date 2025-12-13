package net.exylia.commons.v2.lifecycle;

import lombok.Getter;

@Getter
public enum LifecycleStage {
    PRE_INIT(0),
    LICENSE_VALIDATION(1),
    CORE_INIT(2),
    PLUGIN_INIT(3),
    POST_INIT(4),
    RUNNING(5),
    PRE_SHUTDOWN(6),
    SHUTDOWN(7);

    private final int order;

    LifecycleStage(int order) {
        this.order = order;
    }
}
