package net.exylia.commons.v2.visual.instance;

import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SimpleVisualInstance<T extends VisualConfig> extends VisualInstance<T> {
    private static final long DEFAULT_AUTO_CLEANUP_MILLIS = 3000L;

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
        renderer.cleanup(player, id);
        lifecycle.cancel();
        VisualRegistry.getInstance().remove(player.getUniqueId(), id);
    }

    private void scheduleAutoCleanup() {
        long cleanupDelayMillis = resolveAutoCleanupDelayMillis();

        Tasks.build()
                .run(() -> {
                    renderer.cleanup(player, id);
                    lifecycle.complete();
                    VisualRegistry.getInstance().remove(player.getUniqueId(), id);
                })
                .delay(cleanupDelayMillis, TimeUnit.MILLISECONDS)
                .sync()
                .schedule();
    }

    private long resolveAutoCleanupDelayMillis() {
        if (config instanceof TitleConfig titleConfig) {
            long totalTicks = Math.max(1L, (long) titleConfig.getFadeIn() + titleConfig.getStay() + titleConfig.getFadeOut());
            return totalTicks * 50L;
        }

        return DEFAULT_AUTO_CLEANUP_MILLIS;
    }
}
