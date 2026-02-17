package net.exylia.commons.v2.lifecycle;

import lombok.Getter;

@Getter
public enum LifecycleStage {
    PRE_INIT(0),
    CORE_INIT(1),
    PLUGIN_INIT(2),
    POST_INIT(3),
    RUNNING(4),
    PRE_SHUTDOWN(5),
    SHUTDOWN(6);

    private final int order;

    LifecycleStage(int order) {
        this.order = order;
    }
}
