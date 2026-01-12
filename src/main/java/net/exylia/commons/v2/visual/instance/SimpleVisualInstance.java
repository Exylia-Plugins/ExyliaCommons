package net.exylia.commons.v2.visual.instance;

import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SimpleVisualInstance<T extends VisualConfig> extends VisualInstance<T> {
    private static final long AUTO_CLEANUP_TICKS = 60L;

    public SimpleVisualInstance(
            String id,
            T config,
            Player player,
            PlaceholderContext context,
            VisualRenderer<T> renderer
    ) {
        super(id, config, player, context, renderer);
    }

    @Override
    public CompletableFuture<Void> start() {
        lifecycle.start();

        try {
            render();
            scheduleAutoCleanup();
        } catch (Exception e) {
            lifecycle.error();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void cancel() {
        lifecycle.cancel();
        VisualRegistry.getInstance().remove(player.getUniqueId(), id);
    }

    private void scheduleAutoCleanup() {
        SchedulerManager.getInstance()
                .task(() -> {
                    lifecycle.complete();
                    VisualRegistry.getInstance().remove(player.getUniqueId(), id);
                })
                .delay(AUTO_CLEANUP_TICKS, TimeUnit.MILLISECONDS)
                .schedule();
    }
}
