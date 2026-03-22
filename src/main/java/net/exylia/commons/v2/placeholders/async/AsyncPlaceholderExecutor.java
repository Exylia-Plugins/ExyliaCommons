package net.exylia.commons.v2.placeholders.async;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.model.TaskCategory;

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
        return Tasks.computeValue(task);
    }

    public <T> CompletableFuture<T> executeAsyncDb(Supplier<T> task) {
        return Tasks.dbValue(task);
    }

    public CompletableFuture<String> executeAsyncPlaceholder(Supplier<Object> resolver) {
        return executeAsync(resolver)
                .thenApply(result -> result != null ? result.toString() : "")
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.ASYNC,
                        "Error executing async placeholder: " + throwable.getMessage(), throwable);
                    return "";
                });
    }

    public CompletableFuture<String> executeAsyncDbPlaceholder(Supplier<Object> resolver) {
        return executeAsyncDb(resolver)
                .thenApply(result -> result != null ? result.toString() : "")
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.ASYNC,
                        "Error executing async DB placeholder: " + throwable.getMessage(), throwable);
                    return "";
                });
    }
}
