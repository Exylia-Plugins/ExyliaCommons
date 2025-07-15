package net.exylia.commons.scoreboard.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sistema de métricas para monitorear el rendimiento de scoreboards
 */
public class ScoreboardMetrics {

    private final AtomicInteger scoreboardsShown = new AtomicInteger(0);
    private final AtomicInteger scoreboardsHidden = new AtomicInteger(0);
    private final AtomicInteger updateCycles = new AtomicInteger(0);
    private final AtomicInteger updateErrors = new AtomicInteger(0);

    private final AtomicLong totalUpdateTime = new AtomicLong(0);
    private final AtomicLong maxUpdateTime = new AtomicLong(0);

    public void incrementScoreboardsShown() {
        scoreboardsShown.incrementAndGet();
    }

    public void incrementScoreboardsHidden() {
        scoreboardsHidden.incrementAndGet();
    }

    public void recordUpdateCycle(int updated, int errors, long durationNanos) {
        updateCycles.incrementAndGet();
        updateErrors.addAndGet(errors);

        long currentTotal = totalUpdateTime.addAndGet(durationNanos);

        // Actualizar tiempo máximo
        maxUpdateTime.updateAndGet(current -> Math.max(current, durationNanos));
    }

    /**
     * Obtiene un resumen de las métricas
     */
    public ScoreboardStats getStats() {
        int cycles = updateCycles.get();
        double avgUpdateTime = cycles > 0 ?
                (totalUpdateTime.get() / cycles) / 1_000_000.0 : 0;

        return new ScoreboardStats(
                scoreboardsShown.get(),
                scoreboardsHidden.get(),
                cycles,
                updateErrors.get(),
                avgUpdateTime,
                maxUpdateTime.get() / 1_000_000.0
        );
    }

    /**
     * Resetea todas las métricas
     */
    public void reset() {
        scoreboardsShown.set(0);
        scoreboardsHidden.set(0);
        updateCycles.set(0);
        updateErrors.set(0);
        totalUpdateTime.set(0);
        maxUpdateTime.set(0);
    }

    public static class ScoreboardStats {
        public final int scoreboardsShown;
        public final int scoreboardsHidden;
        public final int updateCycles;
        public final int updateErrors;
        public final double avgUpdateTimeMs;
        public final double maxUpdateTimeMs;

        public ScoreboardStats(int shown, int hidden, int cycles, int errors,
                               double avgTime, double maxTime) {
            this.scoreboardsShown = shown;
            this.scoreboardsHidden = hidden;
            this.updateCycles = cycles;
            this.updateErrors = errors;
            this.avgUpdateTimeMs = avgTime;
            this.maxUpdateTimeMs = maxTime;
        }

        @Override
        public String toString() {
            return String.format(
                    "ScoreboardStats{shown=%d, hidden=%d, cycles=%d, errors=%d, avgTime=%.2fms, maxTime=%.2fms}",
                    scoreboardsShown, scoreboardsHidden, updateCycles, updateErrors,
                    avgUpdateTimeMs, maxUpdateTimeMs
            );
        }
    }
}