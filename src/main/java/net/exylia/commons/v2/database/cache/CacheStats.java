package net.exylia.commons.v2.database.cache;

import lombok.Getter;

@Getter
public class CacheStats {

    private final long hits;
    private final long misses;
    private final long evictions;
    private final long loads;
    private final long size;

    public CacheStats(long hits, long misses, long evictions, long loads, long size) {
        this.hits = hits;
        this.misses = misses;
        this.evictions = evictions;
        this.loads = loads;
        this.size = size;
    }

    public double hitRate() {
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total * 100;
    }

    public double missRate() {
        long total = hits + misses;
        return total == 0 ? 0 : (double) misses / total * 100;
    }

    @Override
    public String toString() {
        return String.format("CacheStats{hits=%d, misses=%d, rate=%.2f%%, evictions=%d, loads=%d, size=%d}",
                hits, misses, hitRate(), evictions, loads, size);
    }
}
