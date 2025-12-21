package net.exylia.commons.v2.visual.instance;

import lombok.Getter;
import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.formatter.api.FormatterAPI;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.visual.core.GlobalVisualRegistry;
import net.exylia.commons.v2.visual.renderer.BossBarRenderer;
import net.exylia.commons.v2.visual.renderer.ActionBarRenderer;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class GlobalCountdownInstance<T extends VisualConfig> {
    private final String id;
    private final T config;
    private final long initialDurationTicks;
    private final VisualRenderer<T> renderer;
    private final InstanceLifecycle lifecycle;
    private final PlaceholderContext baseContext;

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private long ticksRemaining;
    private ScheduledTask countdownTask;

    private Predicate<Player> playerFilter;
    private Consumer<GlobalCountdownContext> onComplete;
    private Consumer<GlobalCountdownContext> onCancel;
    private Consumer<GlobalCountdownContext> onTick;

    public GlobalCountdownInstance(
            String id,
            T config,
            long durationTicks,
            PlaceholderContext baseContext,
            VisualRenderer<T> renderer,
            Predicate<Player> playerFilter,
            Consumer<GlobalCountdownContext> onComplete,
            Consumer<GlobalCountdownContext> onCancel,
            Consumer<GlobalCountdownContext> onTick
    ) {
        this.id = id;
        this.config = config;
        this.initialDurationTicks = durationTicks;
        this.ticksRemaining = durationTicks;
        this.baseContext = baseContext;
        this.renderer = renderer;
        this.lifecycle = new InstanceLifecycle(id);
        this.playerFilter = playerFilter;
        this.onComplete = onComplete;
        this.onCancel = onCancel;
        this.onTick = onTick;
    }

    public CompletableFuture<Void> start() {
        if (lifecycle.getState() == InstanceLifecycle.LifecycleState.PAUSED) {
            lifecycle.resume();
        } else {
            lifecycle.start();
        }

        countdownTask = Schedulers.syncTimer(this::tick, 0L, 1L);

        return CompletableFuture.completedFuture(null);
    }

    private void tick() {
        if (lifecycle.getState() != InstanceLifecycle.LifecycleState.ACTIVE) {
            return;
        }

        refreshPlayers();

        if (viewers.isEmpty()) {
            cancel();
            return;
        }

        if (ticksRemaining <= 0) {
            complete();
            return;
        }

        PlaceholderContext updateContext = createCurrentContext();

        renderToAllViewers(updateContext)
                .exceptionally(throwable -> null);

        if (onTick != null) {
            GlobalCountdownContext ctx = new GlobalCountdownContext(this, updateContext);
            try {
                onTick.accept(ctx);
            } catch (Exception ignored) {
            }
        }

        ticksRemaining--;
    }

    private PlaceholderContext createCurrentContext() {
        long secondsRemaining = (ticksRemaining + 19) / 20;
        long millisRemaining = ticksRemaining * 50;
        double progress = initialDurationTicks > 0
                ? (double) ticksRemaining / initialDurationTicks
                : 0.0;

        return baseContext.copy()
                .put("time", secondsRemaining)
                .put("time_formatted", FormatterAPI.formatTime(millisRemaining))
                .put("ticks_remaining", ticksRemaining)
                .put("progress", progress)
                .put("countdown_active", true)
                .withCurrentTime();
    }

    private CompletableFuture<Void> renderToAllViewers(PlaceholderContext updateContext) {
        Collection<Player> onlinePlayers = viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .filter(p -> playerFilter == null || playerFilter.test(p))
                .toList();

        if (onlinePlayers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (renderer instanceof BossBarRenderer bossBarRenderer && config instanceof BossBarConfig bossBarConfig) {
            return bossBarRenderer.renderBatch(onlinePlayers, bossBarConfig, updateContext);
        } else if (renderer instanceof ActionBarRenderer actionBarRenderer && config instanceof ActionBarConfig actionBarConfig) {
            return actionBarRenderer.renderBatch(onlinePlayers, actionBarConfig, updateContext);
        }

        List<CompletableFuture<Void>> futures = onlinePlayers.stream()
                .map(player -> renderer.renderAsync(player, config, updateContext))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void refreshPlayers() {
        viewers.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            return player == null || !player.isOnline();
        });
    }

    public void restart() {
        ticksRemaining = initialDurationTicks;
        if (lifecycle.getState() == InstanceLifecycle.LifecycleState.PAUSED) {
            lifecycle.resume();
        }
    }

    public void pause() {
        if (lifecycle.getState() != InstanceLifecycle.LifecycleState.ACTIVE) {
            return;
        }

        lifecycle.pause();

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
    }

    public void resume() {
        if (lifecycle.getState() != InstanceLifecycle.LifecycleState.PAUSED) {
            return;
        }

        start();
    }

    public void cancel() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> renderer.cleanup(p, id));

        if (onCancel != null) {
            GlobalCountdownContext ctx = new GlobalCountdownContext(this, baseContext);
            try {
                Schedulers.sync(() -> onCancel.accept(ctx));
            } catch (Exception ignored) {
            }
        }

        lifecycle.cancel();
        GlobalVisualRegistry.getInstance().unregister(id);
    }

    private void complete() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> renderer.cleanup(p, id));

        if (onComplete != null) {
            GlobalCountdownContext ctx = new GlobalCountdownContext(this, baseContext);
            try {
                Schedulers.sync(() -> onComplete.accept(ctx));
            } catch (Exception ignored) {
            }
        }

        lifecycle.complete();
        GlobalVisualRegistry.getInstance().unregister(id);
    }

    public void addPlayer(UUID playerId) {
        viewers.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        viewers.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            renderer.cleanup(player, id);
        }
    }

    public void addAllOnlinePlayers() {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> playerFilter == null || playerFilter.test(p))
                .forEach(p -> addPlayer(p.getUniqueId()));
    }

    public int getDurationSeconds() {
        return (int) ((initialDurationTicks + 19) / 20);
    }

    public boolean isPaused() {
        return lifecycle.getState() == InstanceLifecycle.LifecycleState.PAUSED;
    }

    public boolean isActive() {
        return lifecycle.getState() == InstanceLifecycle.LifecycleState.ACTIVE;
    }

    public boolean isFinished() {
        return lifecycle.isFinished();
    }
}
