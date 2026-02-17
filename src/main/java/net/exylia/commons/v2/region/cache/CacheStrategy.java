package net.exylia.commons.v2.region.cache;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
public enum CacheStrategy {
    FLAG_CACHE(5, TimeUnit.SECONDS, 50_000),
    PLAYER_STATE_CACHE(10, TimeUnit.MINUTES, 5_000);

    private final long duration;
    private final TimeUnit timeUnit;
    private final long maxSize;

    CacheStrategy(long duration, TimeUnit timeUnit, long maxSize) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.maxSize = maxSize;
    }

    public long getDurationInMillis() {
        return timeUnit.toMillis(duration);
    }
}
