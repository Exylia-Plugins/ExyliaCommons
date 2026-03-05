package net.exylia.commons.v2.scoreboard.instance;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.renderer.FastBoardComponentAdapter;
import net.exylia.commons.v2.scoreboard.renderer.ScoreboardRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Getter
public class ScoreboardInstance {

    private final String id;
    private final Player player;
    private final Scoreboard scoreboard;
    private final FastBoardComponentAdapter fastBoardAdapter;
    private final InstanceLifecycle lifecycle;
    private final ScoreboardRenderer renderer;
    private final long createdAt;

    private PlaceholderContext context;
    private long lastUpdate;

    public ScoreboardInstance(
            String id,
            Player player,
            Scoreboard scoreboard,
            FastBoardComponentAdapter fastBoardAdapter,
            ScoreboardRenderer renderer,
            PlaceholderContext context
    ) {
        this.id = id;
        this.player = player;
        this.scoreboard = scoreboard;
        this.fastBoardAdapter = fastBoardAdapter;
        this.renderer = renderer;
        this.context = context != null ? context : PlaceholderContext.create();
        this.lifecycle = new InstanceLifecycle();
        this.createdAt = System.currentTimeMillis();
        this.lastUpdate = 0;
    }

    public CompletableFuture<Void> show() {
        return renderer.renderAsync(player, scoreboard, context, fastBoardAdapter)
                .thenRun(() -> {
                    lifecycle.activate();
                    lastUpdate = System.currentTimeMillis();
                });
    }

    public void hide() {
        lifecycle.cancel();

        if (fastBoardAdapter != null && !fastBoardAdapter.isDeleted()) {
            fastBoardAdapter.delete();
        }

        renderer.cleanup(player);
    }

    public CompletableFuture<Void> update() {
        if (!lifecycle.canUpdate()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!player.isOnline()) {
            lifecycle.cancel();
            return CompletableFuture.completedFuture(null);
        }

        return renderer.renderAsync(player, scoreboard, context, fastBoardAdapter)
                .thenRun(() -> lastUpdate = System.currentTimeMillis());
    }

    public boolean shouldUpdate() {
        if (!lifecycle.canUpdate() || !player.isOnline()) return false;

        long interval = scoreboard.getUpdateInterval() * 50L;
        return System.currentTimeMillis() - lastUpdate >= interval;
    }

    public void updateContext(PlaceholderContext newContext) {
        this.context = newContext != null ? newContext : PlaceholderContext.create();
    }

    public void forceUpdate() {
        this.lastUpdate = 0;
    }

    public void reinitialize() {
        if (!lifecycle.isActive() || fastBoardAdapter.isDeleted()) return;
        fastBoardAdapter.reinitialize();
        update();
    }

    public boolean isActive() {
        return lifecycle.isActive() && player.isOnline();
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }
}
