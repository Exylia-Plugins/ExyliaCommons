package net.exylia.commons.v2.formatter.core;

import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.formatter.cache.FormatterCache;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class AbstractFormatter<I, O> implements Formatter<I, O> {
    protected final FormatterCache cache;

    protected AbstractFormatter(FormatterCache cache) {
        this.cache = cache;
    }

    @Override
    public CompletableFuture<O> formatAsync(I input) {
        return Tasks.computeValue(() -> format(input));
    }

    @Override
    public List<O> formatBatch(List<I> inputs) {
        return inputs.stream()
            .map(this::format)
            .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<O>> formatBatchAsync(List<I> inputs) {
        return Tasks.computeValue(() -> formatBatch(inputs));
    }

    @Override
    public void invalidateCache() {
        cache.invalidateResults();
    }

    protected abstract String getCacheKey(I input, String pattern);
}
