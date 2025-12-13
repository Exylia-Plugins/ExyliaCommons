package net.exylia.commons.v2.reload.stats;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ReloadStats {
    private final boolean success;
    private final long totalDuration;
    private final Map<String, SystemReloadMetrics> systemMetrics;
    private final Map<String, String> skippedSystems;
    private final Map<String, Exception> errors;

    private ReloadStats(Builder builder) {
        this.success = builder.success;
        this.totalDuration = builder.totalDuration;
        this.systemMetrics = builder.systemMetrics;
        this.skippedSystems = builder.skippedSystems;
        this.errors = builder.errors;
    }

    public int getSuccessCount() {
        return (int) systemMetrics.values().stream()
                .filter(SystemReloadMetrics::isSuccess)
                .count();
    }

    public int getFailureCount() {
        return errors.size();
    }

    public int getSkippedCount() {
        return skippedSystems.size();
    }

    public String getFormattedDuration() {
        if (totalDuration < 1000) {
            return totalDuration + "ms";
        } else if (totalDuration < 60000) {
            return String.format("%.2fs", totalDuration / 1000.0);
        } else {
            long minutes = totalDuration / 60000;
            long seconds = (totalDuration % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== RELOAD SYSTEM V2 REPORT ===\n");
        sb.append("§7Status: ").append(success ? "§a✔ Success" : "§c✖ Failed").append("\n");
        sb.append("§7Total Duration: §e").append(getFormattedDuration()).append("\n");
        sb.append("§7Systems: §a").append(getSuccessCount()).append(" success§7, ");
        sb.append("§c").append(getFailureCount()).append(" failed§7, ");
        sb.append("§8").append(getSkippedCount()).append(" skipped\n");

        if (!systemMetrics.isEmpty()) {
            sb.append("\n§7§lDetailed Breakdown:\n");
            systemMetrics.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().getDurationMs(), a.getValue().getDurationMs()))
                    .forEach(entry -> {
                        SystemReloadMetrics metrics = entry.getValue();
                        String status = metrics.isSuccess() ? "§a✔" : "§c✖";
                        sb.append(status).append(" §f").append(metrics.getSystemName())
                                .append("§7: §e").append(metrics.getFormattedDuration())
                                .append(" §8[").append(metrics.getPhase()).append("]\n");
                    });
        }

        if (!skippedSystems.isEmpty()) {
            sb.append("\n§8Skipped Systems:\n");
            skippedSystems.forEach((name, reason) ->
                    sb.append("§8  - ").append(name).append(": ").append(reason).append("\n"));
        }

        if (!errors.isEmpty()) {
            sb.append("\n§cErrors:\n");
            errors.forEach((name, error) ->
                    sb.append("§c  - ").append(name).append(": ").append(error.getMessage()).append("\n"));
        }

        return sb.toString();
    }

    public static class Builder {
        private boolean success = true;
        private long totalDuration;
        private final Map<String, SystemReloadMetrics> systemMetrics = new HashMap<>();
        private final Map<String, String> skippedSystems = new HashMap<>();
        private final Map<String, Exception> errors = new HashMap<>();

        public Builder setTotalDuration(long duration) {
            this.totalDuration = duration;
            return this;
        }

        public Builder add(String systemName, SystemReloadMetrics metrics) {
            systemMetrics.put(systemName, metrics);
            if (!metrics.isSuccess()) {
                success = false;
                metrics.getError().ifPresent(error -> errors.put(systemName, error));
            }
            return this;
        }

        public Builder skip(String systemName, String reason) {
            skippedSystems.put(systemName, reason);
            return this;
        }

        public Builder addError(String systemName, Exception error) {
            errors.put(systemName, error);
            success = false;
            return this;
        }

        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public ReloadStats build() {
            return new ReloadStats(this);
        }
    }
}
