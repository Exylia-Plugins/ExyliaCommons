package net.exylia.commons.v2.visual.instance;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.formatter.api.FormatterAPI;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.core.VisualRegistry;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class CountdownVisualInstance<T extends VisualConfig> extends VisualInstance<T> {
    private final long durationTicks;
    private long ticksRemaining;
    private long ticksSinceRender = 0;
    private ScheduledTask countdownTask;

    @Setter
    private Runnable onComplete;
    @Setter
    private Runnable onCancel;
    @Setter
    private Consumer<Long> onTick;

    public CountdownVisualInstance(
            String id,
            T config,
            Player player,
            PlaceholderContext context,
            VisualRenderer<T> renderer,
            long durationTicks
    ) {
        super(id, config, player, context, renderer);
        this.durationTicks = durationTicks;
        this.ticksRemaining = durationTicks;
    }

    @Override
    public CompletableFuture<Void> start() {
        lifecycle.start();

        PlaceholderContext countdownContext = context.copy();

        countdownTask = Tasks.timer(() -> {
            if (!player.isOnline()) {
                cancel();
                return;
            }

            if (ticksRemaining <= 0) {
                complete();
                return;
            }

            long secondsRemaining = (ticksRemaining + 19) / 20;
            long millisRemaining = ticksRemaining * 50;
            double progress = durationTicks > 0 ? (double) ticksRemaining / durationTicks : 0.0;
            double decimalSeconds = ticksRemaining / 20.0;

            if (ticksRemaining % 20 == 0 && onTick != null) {
                try {
                    onTick.accept(secondsRemaining);
                } catch (Exception ignored) {
                }
            }

            countdownContext.put("time", secondsRemaining);
            countdownContext.put("time_decimal", String.format("%.1f", decimalSeconds));
            countdownContext.put("time_formatted", FormatterAPI.formatTime(millisRemaining));
            countdownContext.put("ticks_remaining", ticksRemaining);
            countdownContext.put("progress", progress);
            countdownContext.put("countdown_active", true);

            updateContext(countdownContext);

            if (ticksSinceRender <= 0) {
                ticksSinceRender = config.getUpdateInterval();
                try {
                    render();
                } catch (Exception ignored) {
                }
            }
            ticksSinceRender--;
            ticksRemaining--;

        }, 0L, 1L);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void cancel() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        renderer.cleanup(player, id);

        // Remove from the registry before invoking onCancel: if the callback
        // restarts a countdown with the same key (common for "loop" style
        // countdowns like AFK zone rewards), sendCountdown() must not find
        // this now-dead instance still registered, or it will just reset()
        // a stopped instance instead of creating a fresh running one.
        lifecycle.cancel();
        VisualRegistry.getInstance().remove(player.getUniqueId(), id);

        if (onCancel != null) {
            try {
                onCancel.run();
            } catch (Exception ignored) {
            }
        }
    }

    private void complete() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        renderer.cleanup(player, id);

        // Same ordering fix as cancel(): unregister before onComplete so that
        // callbacks which immediately re-arm a countdown under the same key
        // (e.g. AFK zone reward loop) always create a brand new instance
        // instead of silently no-op'ing on the completed/dead one.
        lifecycle.complete();
        VisualRegistry.getInstance().remove(player.getUniqueId(), id);

        if (onComplete != null) {
            try {
                onComplete.run();
            } catch (Exception ignored) {
            }
        }
    }

    public void resetDuration(long newDurationTicks) {
        this.ticksRemaining = newDurationTicks;
    }

    public int getDurationSeconds() {
        return (int) ((durationTicks + 19) / 20);
    }

    public CountdownVisualInstance<T> onComplete(Runnable callback) {
        this.onComplete = callback;
        return this;
    }

    public CountdownVisualInstance<T> onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    public CountdownVisualInstance<T> onTick(Consumer<Long> callback) {
        this.onTick = callback;
        return this;
    }

}
