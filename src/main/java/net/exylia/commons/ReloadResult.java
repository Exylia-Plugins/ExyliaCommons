package net.exylia.commons;

import java.util.HashMap;
import java.util.Map;

public class ReloadResult {
    private final boolean success;
    private final long durationMs;
    private final Map<String, Long> componentTimes;
    private final String errorMessage;

    public ReloadResult(boolean success, long durationMs, Map<String, Long> componentTimes, String errorMessage) {
        this.success = success;
        this.durationMs = durationMs;
        this.componentTimes = componentTimes != null ? componentTimes : new HashMap<>();
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public Map<String, Long> getComponentTimes() { return componentTimes; }
    public String getErrorMessage() { return errorMessage; }

    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== REPORTE DE RELOAD ===\n");
        sb.append("§7Estado: ").append(success ? "§a✓ Exitoso" : "§c✗ Error").append("\n");
        sb.append("§7Tiempo total: §e").append(getFormattedDuration()).append("\n");

        if (!componentTimes.isEmpty()) {
            sb.append("§7Desglose por componente:\n");
            componentTimes.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        String component = entry.getKey();
                        long time = entry.getValue();
                        String formattedTime = time < 1000 ? time + "ms" : String.format("%.2fs", time / 1000.0);
                        sb.append("§8  - §f").append(component).append(": §e").append(formattedTime).append("\n");
                    });
        }

        if (!success && errorMessage != null) {
            sb.append("§7Error: §c").append(errorMessage);
        }

        return sb.toString();
    }
}