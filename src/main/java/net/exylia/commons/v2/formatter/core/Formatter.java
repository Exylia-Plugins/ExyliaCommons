package net.exylia.commons.v2.formatter.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Formatter<I, O> {

    O format(I input);

    O format(I input, String customPattern);

    CompletableFuture<O> formatAsync(I input);

    List<O> formatBatch(List<I> inputs);

    CompletableFuture<List<O>> formatBatchAsync(List<I> inputs);

    void invalidateCache();
}
