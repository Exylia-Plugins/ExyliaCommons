package net.exylia.commons.v2.reload.adapter;

import lombok.AllArgsConstructor;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.reload.api.ReloadableSystem;
import net.exylia.commons.v2.reload.core.ReloadPriority;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;

import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public abstract class ReloadableSystemAdapter implements ReloadableSystem {
    protected final String name;
    protected final ReloadPriority priority;

    protected abstract void performReload() throws Exception;

    protected abstract void performCacheClear() throws Exception;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ReloadPriority getPriority() {
        return priority;
    }

    @Override
    public CompletableFuture<SystemReloadMetrics> reload(ReloadContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();

            try {
                performCacheClear();
                performReload();

                long duration = System.currentTimeMillis() - start;
                return SystemReloadMetrics.success(name, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                return SystemReloadMetrics.failure(name, duration, e);
            }
        });
    }
}
