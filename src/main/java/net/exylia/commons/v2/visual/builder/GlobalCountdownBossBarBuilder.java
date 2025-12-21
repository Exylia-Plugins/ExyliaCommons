package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.GlobalCountdownContext;
import net.exylia.commons.v2.visual.renderer.BossBarRenderer;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GlobalCountdownBossBarBuilder {
    private String id;
    private final int durationSeconds;
    private String text = "Tiempo: {time_formatted}";
    private BossBar.Color color = BossBar.Color.BLUE;
    private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
    private PlaceholderContext context = PlaceholderContext.create();
    private Predicate<Player> playerFilter;
    private Consumer<GlobalCountdownContext> onComplete;
    private Consumer<GlobalCountdownContext> onCancel;
    private Consumer<GlobalCountdownContext> onTick;

    public GlobalCountdownBossBarBuilder(String id, int durationSeconds) {
        this.id = id;
        this.durationSeconds = durationSeconds;
    }

    public GlobalCountdownBossBarBuilder text(String text) {
        this.text = text;
        return this;
    }

    public GlobalCountdownBossBarBuilder color(BossBar.Color color) {
        this.color = color;
        return this;
    }

    public GlobalCountdownBossBarBuilder overlay(BossBar.Overlay overlay) {
        this.overlay = overlay;
        return this;
    }

    public GlobalCountdownBossBarBuilder context(PlaceholderContext context) {
        this.context = context;
        return this;
    }

    public GlobalCountdownBossBarBuilder playerFilter(Predicate<Player> filter) {
        this.playerFilter = filter;
        return this;
    }

    public GlobalCountdownBossBarBuilder onComplete(Consumer<GlobalCountdownContext> callback) {
        this.onComplete = callback;
        return this;
    }

    public GlobalCountdownBossBarBuilder onCancel(Consumer<GlobalCountdownContext> callback) {
        this.onCancel = callback;
        return this;
    }

    public GlobalCountdownBossBarBuilder onTick(Consumer<GlobalCountdownContext> callback) {
        this.onTick = callback;
        return this;
    }

    public CompletableFuture<String> start() {
        if (id == null || id.isEmpty()) {
            id = "global_bossbar_" + System.currentTimeMillis();
        }

        BossBarConfig config = BossBarBuilder.create()
                .text(text)
                .color(color)
                .style(overlay)
                .build();

        return VisualManager.getInstance().sendGlobalCountdown(
                id,
                config,
                context,
                BossBarRenderer.getInstance(),
                VisualType.BOSSBAR,
                durationSeconds * 20L,
                playerFilter,
                onComplete,
                onCancel,
                onTick
        );
    }
}
