package net.exylia.commons.v2.visual.instance;

import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class ContinuousVisualInstance<T extends VisualConfig> extends VisualInstance<T> {
    private ScheduledTask updateTask;
    private final AtomicLong updateCounter;

    public ContinuousVisualInstance(
            String id,
            T config,
            Player player,
            PlaceholderContext context,
            VisualRenderer<T> renderer
    ) {
        super(id, config, player, context, renderer);
        this.updateCounter = new AtomicLong(0);
    }

    @Override
    public CompletableFuture<Void> start() {
        lifecycle.start();

        updateTask = Tasks.timer(() -> {
            if (!player.isOnline()) {
                cancel();
                return;
            }

            long count = updateCounter.getAndIncrement();

            PlaceholderContext updateContext = context.copy();
            updateContext.put("update_count", count);
            updateContext.put("permanent_active", true);
            updateContext.withCurrentTime();

            try {
                render();
            } catch (Exception ignored) {
            }

        }, 0L, config.getUpdateInterval());

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void cancel() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        renderer.cleanup(player, id);
        lifecycle.cancel();
        VisualRegistry.getInstance().remove(player.getUniqueId(), id);
    }

    @Override
    public boolean isPermanent() {
        return true;
    }
}
