package net.exylia.commons.wizard.generic;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an active generic wizard session
 */
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

    /**
     * Complete the session with result
     */
    void complete(Object result) {
        if (!future.isDone()) {
            future.complete(result);
        }
    }

    /**
     * Cancel the session
     */
    void cancel() {
        if (!future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * Complete with exception
     */
    void completeExceptionally(Throwable throwable) {
        if (!future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    /**
     * Get the future with correct type
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }

    /**
     * Check if session is still active
     */
    public boolean isActive() {
        return !future.isDone();
    }

    /**
     * Get session duration in milliseconds
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}