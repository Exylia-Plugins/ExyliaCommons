package net.exylia.commons.v2.placeholders.async;

import net.exylia.commons.async.AsyncAPI;
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
        return executeAsync(resolver)
                .thenApply(result -> result != null ? result.toString() : "")
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return "";
                });
    }

    public CompletableFuture<String> executeAsyncDbPlaceholder(Supplier<Object> resolver) {
        return executeAsyncDb(resolver)
                .thenApply(result -> result != null ? result.toString() : "")
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return "";
                });
    }
}
