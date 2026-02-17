package net.exylia.commons.v2.wizard.session;

import lombok.Getter;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.model.WizardType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.concurrent.CompletableFuture;

@Getter
public abstract class WizardSession<T> {
    protected final Player player;
    protected final WizardConfig config;
    protected final CompletableFuture<T> future;
    protected volatile boolean cancelled;

    protected WizardSession(Player player, WizardConfig config) {
        this.player = player;
        this.config = config != null ? config : WizardConfig.defaults();
        this.future = new CompletableFuture<>();
        this.cancelled = false;
    }

    public abstract WizardType getType();

    public abstract void handleInteraction(PlayerInteractEvent event);

    @SuppressWarnings("unchecked")
    public void complete(Object result) {
        if (!cancelled && !future.isDone()) {
            future.complete((T) result);
        }
    }

    public void cancel() {
        cancelled = true;
        if (!future.isDone()) {
            future.cancel(false);
        }
    }

    public void completeExceptionally(Throwable t) {
        if (!cancelled && !future.isDone()) {
            future.completeExceptionally(t);
        }
    }

    public boolean isActive() {
        return !cancelled && !future.isDone();
    }
}
