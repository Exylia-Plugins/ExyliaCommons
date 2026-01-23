package net.exylia.commons.v2.tasks.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.tasks.model.TaskCategory;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class TaskStats {
    private final Map<TaskCategory, CategorySnapshot> categories;

    public CategorySnapshot getCategory(TaskCategory category) {
        return categories.get(category);
    }

    public long getTotalSubmitted() {
        return categories.values().stream().mapToLong(CategorySnapshot::submitted).sum();
    }

    public long getTotalCompleted() {
        return categories.values().stream().mapToLong(CategorySnapshot::completed).sum();
    }

    public long getTotalFailed() {
        return categories.values().stream().mapToLong(CategorySnapshot::failed).sum();
    }

    public long getTotalActive() {
        return categories.values().stream()
            .mapToLong(s -> s.started() - s.completed() - s.failed() - s.timedOut() - s.cancelled())
            .sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TaskStats:\n");
        categories.forEach((cat, snapshot) -> {
            sb.append(String.format("  %s: %d submitted, %d completed, %d failed, avg %.2fms\n",
                cat.getDisplayName(),
                snapshot.submitted(),
                snapshot.completed(),
                snapshot.failed(),
                snapshot.avgDurationMs()));
        });
        sb.append(String.format("  Total: %d submitted, %d completed, %d failed, %d active",
            getTotalSubmitted(), getTotalCompleted(), getTotalFailed(), getTotalActive()));
        return sb.toString();
    }

    public record CategorySnapshot(
        long submitted,
        long started,
        long completed,
        long failed,
        long timedOut,
        long cancelled,
        long rejected,
        double avgDurationMs,
        double minDurationMs,
        double maxDurationMs
    ) {
        public long pending() {
            return submitted - started;
        }

        public long active() {
            return started - completed - failed - timedOut - cancelled;
        }

        public double successRate() {
            long total = completed + failed;
            return total > 0 ? (completed * 100.0) / total : 100.0;
        }
    }
}
