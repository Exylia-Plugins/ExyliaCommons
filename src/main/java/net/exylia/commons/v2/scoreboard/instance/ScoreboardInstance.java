package net.exylia.commons.v2.scoreboard.instance;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.render.ScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.render.ScoreboardRenderer.RenderedBoard;
import net.exylia.commons.v2.scoreboard.protocol.PacketScoreboardSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public final class ScoreboardInstance {

    private static final String LOG_PREFIX = "[Scoreboard] [ScoreboardInstance] ";

    private final UUID playerId;
    private final Player player;
    private final Scoreboard scoreboard;
    private final ScoreboardRenderer renderer;
    private final String objective;
    private final long createdAt;
    private final AtomicLong totalUpdates = new AtomicLong();
    private final AtomicLong totalRenderNanos = new AtomicLong();
    private final AtomicBoolean active = new AtomicBoolean();

    private volatile PlaceholderContext context;
    private volatile RenderedBoard renderedBoard;
    private volatile long lastUpdateNanos;

    public ScoreboardInstance(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.scoreboard = scoreboard;
        this.context = context == null ? PlaceholderContext.create() : context;
        this.objective = objectiveName(scoreboard.getId());
        this.renderer = new ScoreboardRenderer(scoreboard, objective);
        this.createdAt = System.currentTimeMillis();
        Bukkit.getLogger().info(LOG_PREFIX + "created for " + player.getName() + " (scoreboardId=" + scoreboard.getId()
                + ", objective=" + objective + ", lines=" + (scoreboard.getLines() == null ? 0 : scoreboard.getLines().size()) + ")");
    }

    public CompletableFuture<String> show() {
        boolean ready = PacketScoreboardSender.isReady(player);
        if (!ready || !scoreboard.isEnabled()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "show(" + player.getName() + ") FAILED -> ready=" + ready
                    + ", scoreboard.isEnabled()=" + scoreboard.isEnabled());
            return CompletableFuture.failedFuture(new IllegalStateException("PacketEvents is not available or scoreboard is disabled"));
        }
        long started = System.nanoTime();
        try {
            RenderedBoard result = renderer.render(player, context);
            Bukkit.getLogger().info(LOG_PREFIX + "show(" + player.getName() + ") -> rendered " + result.lines().size()
                    + " lines, sending objective=" + objective + " packets...");
            renderer.create(player, result);
            renderedBoard = result;
            active.set(true);
            lastUpdateNanos = System.nanoTime();
            totalRenderNanos.addAndGet(System.nanoTime() - started);
            Bukkit.getLogger().info(LOG_PREFIX + "show(" + player.getName() + ") SUCCEEDED (active=true, objective=" + objective + ")");
            return CompletableFuture.completedFuture(scoreboard.getId());
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "show(" + player.getName() + ") THREW EXCEPTION -> "
                    + t.getClass().getSimpleName() + " - " + t.getMessage());
            return CompletableFuture.failedFuture(t);
        }
    }

    public void hide() {
        if (!active.getAndSet(false)) {
            Bukkit.getLogger().info(LOG_PREFIX + "hide(" + player.getName() + ") -> instance already inactive, skipping");
            return;
        }
        RenderedBoard previous = renderedBoard;
        int lineCount = previous == null ? scoreboard.getLines().size() : previous.lines().size();
        Bukkit.getLogger().info(LOG_PREFIX + "hide(" + player.getName() + ") -> removing objective=" + objective + " (" + lineCount + " lines)");
        renderer.destroy(player, lineCount);
        renderedBoard = null;
    }

    public void update() {
        if (!active.get()) {
            return;
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "update(" + player.getName() + ") skipped -> player is not online");
            return;
        }
        if (!PacketScoreboardSender.isReady(player)) {
            Bukkit.getLogger().warning(LOG_PREFIX + "update(" + player.getName() + ") skipped -> PacketScoreboardSender.isReady()=false");
            return;
        }
        long started = System.nanoTime();
        RenderedBoard next = renderer.render(player, context);
        RenderedBoard previous = renderedBoard;
        if (!scoreboard.isSmartUpdate() || previous == null || !next.equals(previous)) {
            renderer.update(player, previous, next);
            renderedBoard = next;
            totalUpdates.incrementAndGet();
        }
        lastUpdateNanos = System.nanoTime();
        totalRenderNanos.addAndGet(System.nanoTime() - started);
    }

    /**
     * Re-sends the full board from scratch. Needed after another system (TAB
     * loading the player, world change, respawn) wipes the client-side
     * scoreboard state that our packets created.
     */
    public void reinitialize() {
        if (!active.get()) {
            Bukkit.getLogger().info(LOG_PREFIX + "reinitialize(" + player.getName() + ") skipped -> instance inactive");
            return;
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "reinitialize(" + player.getName() + ") skipped -> player is not online");
            return;
        }
        if (!PacketScoreboardSender.isReady(player)) {
            Bukkit.getLogger().warning(LOG_PREFIX + "reinitialize(" + player.getName() + ") skipped -> PacketScoreboardSender.isReady()=false");
            return;
        }
        RenderedBoard previous = renderedBoard;
        int lineCount = previous == null ? scoreboard.getLines().size() : previous.lines().size();
        Bukkit.getLogger().info(LOG_PREFIX + "reinitialize(" + player.getName() + ") -> destroying and re-sending objective=" + objective + " (" + lineCount + " lines)");
        renderer.destroy(player, lineCount);
        RenderedBoard result = renderer.render(player, context);
        renderer.create(player, result);
        renderedBoard = result;
        lastUpdateNanos = System.nanoTime();
    }

    public void updateContext(PlaceholderContext context) {
        this.context = context == null ? PlaceholderContext.create() : context;
    }

    public void forceUpdate() {
        lastUpdateNanos = 0L;
    }

    public boolean shouldUpdate(long nowNanos) {
        return active.get() && nowNanos - lastUpdateNanos >= scoreboard.getUpdateInterval() * 50_000_000L;
    }

    public boolean isActive() {
        return active.get() && player.isOnline();
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    public double getAverageRenderTimeMs() {
        long updates = totalUpdates.get();
        return updates == 0 ? 0.0 : (totalRenderNanos.get() / (double) updates) / 1_000_000.0;
    }

    private static String objectiveName(String id) {
        String normalized = id == null ? UUID.randomUUID().toString() : id.replaceAll("[^A-Za-z0-9_-]", "");
        if (normalized.isBlank()) normalized = "scoreboard";
        String prefix = "exy_";
        String result = prefix + normalized;
        return result.substring(0, Math.min(16, result.length()));
    }
}
