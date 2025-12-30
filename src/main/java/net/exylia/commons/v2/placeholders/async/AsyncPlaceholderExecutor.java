package net.exylia.commons.v2.placeholders.async;

import net.exylia.commons.async.AsyncAPI;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncPlaceholderExecutor {
    private static final AsyncPlaceholderExecutor INSTANCE = new AsyncPlaceholderExecutor();

    private AsyncPlaceholderExecutor() {
    }

    public static AsyncPlaceholderExecutor getInstance() {
        return INSTANCE;
    }

    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return AsyncAPI.compute(task);
    }

    public <T> CompletableFuture<T> executeAsyncDb(Supplier<T> task) {
        return AsyncAPI.computeDb(task);
    }

    public CompletableFuture<String> executeAsyncPlaceholder(Supplier<Object> resolver) {
        long startTime = System.nanoTime();
        return executeAsync(resolver)
                .thenApply(result -> {
                    double millis = (System.nanoTime() - startTime) / 1_000_000.0;
                    DebugAPI.logLibDebug(DebugCategory.ASYNC,
                        String.format("Async placeholder executed in %.3fms", millis));
                    return result != null ? result.toString() : "";
                })
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.ASYNC,
                        "Error executing async placeholder: " + throwable.getMessage(), throwable);
                    return "";
                });
    }

    public CompletableFuture<String> executeAsyncDbPlaceholder(Supplier<Object> resolver) {
        long startTime = System.nanoTime();
        return executeAsyncDb(resolver)
                .thenApply(result -> {
                    double millis = (System.nanoTime() - startTime) / 1_000_000.0;
                    DebugAPI.logLibDebug(DebugCategory.ASYNC,
                        String.format("Async DB placeholder executed in %.3fms", millis));
                    return result != null ? result.toString() : "";
                })
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.ASYNC,
                        "Error executing async DB placeholder: " + throwable.getMessage(), throwable);
                    return "";
                });
    }
}
