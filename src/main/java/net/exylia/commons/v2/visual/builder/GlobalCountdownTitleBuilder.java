package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.GlobalCountdownContext;
import net.exylia.commons.v2.visual.renderer.TitleRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GlobalCountdownTitleBuilder {
    private final String id;
    private final int durationSeconds;
    private final TitleConfig config;
    private PlaceholderContext context = PlaceholderContext.create();
    private Predicate<Player> playerFilter;
    private Consumer<GlobalCountdownContext> onComplete;
    private Consumer<GlobalCountdownContext> onCancel;
    private Consumer<GlobalCountdownContext> onTick;

    public GlobalCountdownTitleBuilder(String id, int durationSeconds, TitleConfig config) {
        this.id = id;
        this.durationSeconds = durationSeconds;
        this.config = config;
    }

    public GlobalCountdownTitleBuilder context(PlaceholderContext context) {
        this.context = context;
        return this;
    }

    public GlobalCountdownTitleBuilder playerFilter(Predicate<Player> filter) {
        this.playerFilter = filter;
        return this;
    }

    public GlobalCountdownTitleBuilder onComplete(Consumer<GlobalCountdownContext> callback) {
        this.onComplete = callback;
        return this;
    }

    public GlobalCountdownTitleBuilder onCancel(Consumer<GlobalCountdownContext> callback) {
        this.onCancel = callback;
        return this;
    }

    public GlobalCountdownTitleBuilder onTick(Consumer<GlobalCountdownContext> callback) {
        this.onTick = callback;
        return this;
    }

    public CompletableFuture<String> start() {
        return VisualManager.getInstance().sendGlobalCountdown(
                id,
                config,
                context,
                TitleRenderer.getInstance(),
                VisualType.TITLE,
                durationSeconds * 20L,
                playerFilter,
                onComplete,
                onCancel,
                onTick
        );
    }
}
