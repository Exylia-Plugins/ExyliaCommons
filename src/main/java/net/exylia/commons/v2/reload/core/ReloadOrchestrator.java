package net.exylia.commons.v2.reload.core;

import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.reload.api.ReloadableSystem;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReloadOrchestrator {

    public CompletableFuture<SystemReloadMetrics> executeReload(
            ReloadableSystem system,
            ReloadContext context
    ) {
        return system.reload(context)
                .orTimeout(system.getTimeoutSeconds(), TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        return SystemReloadMetrics.failure(
                                system.getName(),
                                system.getTimeoutSeconds() * 1000,
                                new Exception("Timeout after " + system.getTimeoutSeconds() + "s")
                        );
                    }
                    return SystemReloadMetrics.failure(
                            system.getName(),
                            0,
                            throwable instanceof Exception ? (Exception) throwable : new Exception(throwable)
                    );
                });
    }
}
