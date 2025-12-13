package net.exylia.commons.v2.reload.api;

import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;

import java.util.concurrent.CompletableFuture;

public interface ReloadableSystem {
    String getName();

    ReloadPriority getPriority();

    CompletableFuture<SystemReloadMetrics> reload(ReloadContext context);

    default boolean isAvailable() {
        return true;
    }

    default long getTimeoutSeconds() {
        return 30L;
    }

    default boolean isCritical() {
        return false;
    }
}
