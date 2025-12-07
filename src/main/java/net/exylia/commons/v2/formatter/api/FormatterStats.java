package net.exylia.commons.v2.formatter.api;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public class FormatterStats {
    private final AtomicLong formatCount = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalTimeNanos = new AtomicLong(0);

    public void recordFormat(long durationNanos, boolean cacheHit) {
        formatCount.incrementAndGet();
        if (cacheHit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        totalTimeNanos.addAndGet(durationNanos);
    }

    public double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total == 0 ? 0.0 : (double) cacheHits.get() / total;
    }

    public double getAverageTimeMillis() {
        long count = formatCount.get();
        return count == 0 ? 0.0 : (double) totalTimeNanos.get() / count / 1_000_000;
    }

    public void reset() {
        formatCount.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        totalTimeNanos.set(0);
    }
}
