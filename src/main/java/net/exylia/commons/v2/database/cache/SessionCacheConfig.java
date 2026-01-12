package net.exylia.commons.v2.database.cache;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionCacheConfig {
    @Builder.Default
    private final ExpirationMode mode = ExpirationMode.TIMED;

    @Builder.Default
    private final long writeExpirationMinutes = 30;

    @Builder.Default
    private final long accessExpirationMinutes = 15;

    @Builder.Default
    private final int maxSize = 1000;

    @Builder.Default
    private final boolean enableStats = true;

    @Builder.Default
    private final long sessionTTLMinutes = 60;

    @Builder.Default
    private final long cleanupIntervalMinutes = 10;

    @Builder.Default
    private final boolean enableBatchWrite = false;

    @Builder.Default
    private final long batchWriteIntervalMinutes = 5;

    public static SessionCacheConfig defaultConfig() {
        return SessionCacheConfig.builder().build();
    }

    public static SessionCacheConfig activeMode() {
        return SessionCacheConfig.builder()
                .mode(ExpirationMode.ACTIVE)
                .enableBatchWrite(true)
                .build();
    }

    public static SessionCacheConfig timedMode(long writeMinutes, long accessMinutes) {
        return SessionCacheConfig.builder()
                .mode(ExpirationMode.TIMED)
                .writeExpirationMinutes(writeMinutes)
                .accessExpirationMinutes(accessMinutes)
                .build();
    }
}
