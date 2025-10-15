package net.exylia.commons.wizard.generic;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Getter
public class GenericWizardSession {

    private final Player player;
    private final GenericWizardHandler<?> handler;
    private final CompletableFuture<Object> future;
    private final long startTime;

    GenericWizardSession(Player player, GenericWizardHandler<?> handler) {
        this.player = player;
        this.handler = handler;
        this.future = new CompletableFuture<>();
        this.startTime = System.currentTimeMillis();
    }

    void complete(Object result) {
        if (!future.isDone()) {
            future.complete(result);
        }
    }

    void cancel() {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }

    void completeExceptionally(Throwable throwable) {
        if (!future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }

    public boolean isActive() {
        return !future.isDone();
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
