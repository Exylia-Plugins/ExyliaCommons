package net.exylia.commons.v2.tasks.stats;

import lombok.Getter;
import net.exylia.commons.v2.tasks.model.TaskCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Getter
public class TaskMetrics {
    private final Map<TaskCategory, CategoryMetrics> categoryMetrics = new EnumMap<>(TaskCategory.class);

    public TaskMetrics() {
        for (TaskCategory category : TaskCategory.values()) {
            categoryMetrics.put(category, new CategoryMetrics());
        }
    }

    public CategoryMetrics getMetrics(TaskCategory category) {
        return categoryMetrics.get(category);
    }

    public void recordSubmit(TaskCategory category) {
        getMetrics(category).submitted.increment();
    }

    public void recordStart(TaskCategory category) {
        getMetrics(category).started.increment();
    }

    public void recordComplete(TaskCategory category, long durationNanos) {
        CategoryMetrics metrics = getMetrics(category);
        metrics.completed.increment();
        metrics.totalDurationNanos.add(durationNanos);
        metrics.updateMinMax(durationNanos);
    }

    public void recordFailure(TaskCategory category) {
        getMetrics(category).failed.increment();
    }

    public void recordTimeout(TaskCategory category) {
        getMetrics(category).timedOut.increment();
    }

    public void recordCancelled(TaskCategory category) {
        getMetrics(category).cancelled.increment();
    }

    public void recordRejected(TaskCategory category) {
        getMetrics(category).rejected.increment();
    }

    public void reset() {
        categoryMetrics.values().forEach(CategoryMetrics::reset);
    }

    public TaskStats snapshot() {
        Map<TaskCategory, TaskStats.CategorySnapshot> snapshots = new EnumMap<>(TaskCategory.class);
        categoryMetrics.forEach((cat, metrics) -> snapshots.put(cat, metrics.snapshot()));
        return new TaskStats(snapshots);
    }

    @Getter
    public static class CategoryMetrics {
        private final LongAdder submitted = new LongAdder();
        private final LongAdder started = new LongAdder();
        private final LongAdder completed = new LongAdder();
        private final LongAdder failed = new LongAdder();
        private final LongAdder timedOut = new LongAdder();
        private final LongAdder cancelled = new LongAdder();
        private final LongAdder rejected = new LongAdder();
        private final LongAdder totalDurationNanos = new LongAdder();
        private volatile long minDurationNanos = Long.MAX_VALUE;
        private volatile long maxDurationNanos = 0;

        synchronized void updateMinMax(long durationNanos) {
            if (durationNanos < minDurationNanos) minDurationNanos = durationNanos;
            if (durationNanos > maxDurationNanos) maxDurationNanos = durationNanos;
        }

        void reset() {
            submitted.reset();
            started.reset();
            completed.reset();
            failed.reset();
            timedOut.reset();
            cancelled.reset();
            rejected.reset();
            totalDurationNanos.reset();
            minDurationNanos = Long.MAX_VALUE;
            maxDurationNanos = 0;
        }

        TaskStats.CategorySnapshot snapshot() {
            long completedCount = completed.sum();
            double avgDurationMs = completedCount > 0
                ? (totalDurationNanos.sum() / (double) completedCount) / 1_000_000.0
                : 0;

            return new TaskStats.CategorySnapshot(
                submitted.sum(),
                started.sum(),
                completedCount,
                failed.sum(),
                timedOut.sum(),
                cancelled.sum(),
                rejected.sum(),
                avgDurationMs,
                minDurationNanos == Long.MAX_VALUE ? 0 : minDurationNanos / 1_000_000.0,
                maxDurationNanos / 1_000_000.0
            );
        }
    }
}
