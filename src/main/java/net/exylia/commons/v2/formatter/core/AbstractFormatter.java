package net.exylia.commons.v2.formatter.core;

import lombok.Getter;
import net.exylia.commons.async.AsyncAPI;
import net.exylia.commons.v2.formatter.api.FormatterStats;
import net.exylia.commons.v2.formatter.cache.FormatterCache;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
public abstract class AbstractFormatter<I, O> implements Formatter<I, O> {
    protected final FormatterCache cache;
    protected final FormatterStats stats;

    protected AbstractFormatter(FormatterCache cache) {
        this.cache = cache;
        this.stats = new FormatterStats();
    }

    @Override
    public CompletableFuture<O> formatAsync(I input) {
        return AsyncAPI.compute(() -> format(input));
    }

    @Override
    public List<O> formatBatch(List<I> inputs) {
        return inputs.stream()
            .map(this::format)
            .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<O>> formatBatchAsync(List<I> inputs) {
        return AsyncAPI.compute(() -> formatBatch(inputs));
    }

    @Override
    public void invalidateCache() {
        cache.invalidateResults();
    }

    @Override
    public FormatterStats getStats() {
        return stats;
    }

    protected abstract String getCacheKey(I input, String pattern);
}
