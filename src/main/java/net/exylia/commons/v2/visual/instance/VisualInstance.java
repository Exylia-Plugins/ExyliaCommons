package net.exylia.commons.v2.visual.instance;

import lombok.Getter;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Getter
public abstract class VisualInstance<T extends VisualConfig> {
    protected final String id;
    protected T config;
    protected final Player player;
    protected final VisualRenderer<T> renderer;
    protected final InstanceLifecycle lifecycle;
    protected final long createdAt;
    protected PlaceholderContext context;

    protected VisualInstance(
            String id,
            T config,
            Player player,
            PlaceholderContext context,
            VisualRenderer<T> renderer
    ) {
        this.id = id;
        this.config = config;
        this.player = player;
        this.context = context;
        this.renderer = renderer;
        this.lifecycle = new InstanceLifecycle(id);
        this.createdAt = System.currentTimeMillis();
    }

    public abstract CompletableFuture<Void> start();

    public abstract void cancel();

    public void updateContext(PlaceholderContext newContext) {
        this.context = newContext != null ? newContext : PlaceholderContext.create();
    }

    public void updateConfig(T newConfig) {
        if (newConfig != null) {
            this.config = newConfig;
        }
    }

    public boolean isActive() {
        return lifecycle.getState() == InstanceLifecycle.LifecycleState.ACTIVE;
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    public void render() {
        renderer.render(player, config, context);
    }
}
