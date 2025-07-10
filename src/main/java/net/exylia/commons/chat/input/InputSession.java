package net.exylia.commons.chat.input;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an active input session
 */
@Getter
class InputSession {

    private final Player player;
    private final InputHandler<?> handler;
    private final CompletableFuture<Object> future;
    private final long startTime;

    InputSession(Player player, InputHandler<?> handler) {
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
    <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }
}