package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.core.VisualType;
import net.exylia.commons.v2.visual.instance.GlobalCountdownContext;
import net.exylia.commons.v2.visual.renderer.ActionBarRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GlobalCountdownActionBarBuilder {
    private String id;
    private final int durationSeconds;
    private String text = "Tiempo: %time_formatted%";
    private PlaceholderContext context = PlaceholderContext.create();
    private Predicate<Player> playerFilter;
    private Consumer<GlobalCountdownContext> onComplete;
    private Consumer<GlobalCountdownContext> onCancel;
    private Consumer<GlobalCountdownContext> onTick;

    public GlobalCountdownActionBarBuilder(String id, int durationSeconds) {
        this.id = id;
        this.durationSeconds = durationSeconds;
    }

    public GlobalCountdownActionBarBuilder text(String text) {
        this.text = text;
        return this;
    }

    public GlobalCountdownActionBarBuilder context(PlaceholderContext context) {
        this.context = context;
        return this;
    }

    public GlobalCountdownActionBarBuilder playerFilter(Predicate<Player> filter) {
        this.playerFilter = filter;
        return this;
    }

    public GlobalCountdownActionBarBuilder onComplete(Consumer<GlobalCountdownContext> callback) {
        this.onComplete = callback;
        return this;
    }

    public GlobalCountdownActionBarBuilder onCancel(Consumer<GlobalCountdownContext> callback) {
        this.onCancel = callback;
        return this;
    }

    public GlobalCountdownActionBarBuilder onTick(Consumer<GlobalCountdownContext> callback) {
        this.onTick = callback;
        return this;
    }

    public CompletableFuture<String> start() {
        if (id == null || id.isEmpty()) {
            id = "global_actionbar_" + System.currentTimeMillis();
        }

        ActionBarConfig config = ActionBarBuilder.create()
                .text(text)
                .build();

        return VisualManager.getInstance().sendGlobalCountdown(
                id,
                config,
                context,
                ActionBarRenderer.getInstance(),
                VisualType.ACTIONBAR,
                durationSeconds * 20L,
                playerFilter,
                onComplete,
                onCancel,
                onTick
        );
    }
}
