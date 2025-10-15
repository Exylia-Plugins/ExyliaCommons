package net.exylia.commons.chat.input;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

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
    <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }
}
