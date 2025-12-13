package net.exylia.commons.v2.reload.stats;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemReloadMetrics {
    private final String systemName;
    private final boolean success;
    private final long durationMs;
    private final String phase;
    private final Optional<Exception> error;

    public static SystemReloadMetrics success(String systemName, long durationMs) {
        return new SystemReloadMetrics(systemName, true, durationMs, "completed", Optional.empty());
    }

    public static SystemReloadMetrics failure(String systemName, long durationMs, Exception error) {
        return new SystemReloadMetrics(systemName, false, durationMs, "failed", Optional.of(error));
    }

    public static SystemReloadMetrics skipped(String systemName, String reason) {
        return new SystemReloadMetrics(systemName, false, 0, "skipped: " + reason, Optional.empty());
    }

    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else {
            return String.format("%.2fs", durationMs / 1000.0);
        }
    }
}
