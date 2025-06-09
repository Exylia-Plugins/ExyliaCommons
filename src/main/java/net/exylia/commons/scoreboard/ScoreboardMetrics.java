package net.exylia.commons.scoreboard;

import java.util.concurrent.atomic.AtomicInteger;

public class ScoreboardMetrics {
    private final AtomicInteger templatesRegistered = new AtomicInteger(0);
    private final AtomicInteger scoreboardsShown = new AtomicInteger(0);
    private final AtomicInteger scoreboardsHidden = new AtomicInteger(0);
    private final AtomicInteger updateCycles = new AtomicInteger(0);
    private final AtomicInteger updateErrors = new AtomicInteger(0);
    private long totalUpdateTime = 0;
    private long maxUpdateTime = 0;

    public void incrementTemplatesRegistered() {
        templatesRegistered.incrementAndGet();
    }

    public void incrementScoreboardsShown() {
        scoreboardsShown.incrementAndGet();
    }

    public void incrementScoreboardsHidden() {
        scoreboardsHidden.incrementAndGet();
    }

    public synchronized void recordUpdateCycle(int updated, int errors, long durationNanos) {
        updateCycles.incrementAndGet();
        updateErrors.addAndGet(errors);
        totalUpdateTime += durationNanos;
        if (durationNanos > maxUpdateTime) {
            maxUpdateTime = durationNanos;
        }
    }

    public String getSummary() {
        int cycles = updateCycles.get();
        double avgUpdateTime = cycles > 0 ? (totalUpdateTime / cycles) / 1_000_000.0 : 0;

        return String.format(
                "Templates: %d, Mostrados: %d, Ocultados: %d, Ciclos: %d, Errores: %d, Tiempo avg: %.2fms, Max: %.2fms",
                templatesRegistered.get(),
                scoreboardsShown.get(),
                scoreboardsHidden.get(),
                cycles,
                updateErrors.get(),
                avgUpdateTime,
                maxUpdateTime / 1_000_000.0
        );
    }
}