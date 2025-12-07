package net.exylia.commons;

import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.visuals.MessageUtils;

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

    public String getTimingBreakdown() {
        if (componentTimes.isEmpty()) {
            return "No timing data available";
        }

        StringBuilder sb = new StringBuilder();

        long configTime = componentTimes.getOrDefault("Configuración", 0L);
        long dbTime = componentTimes.getOrDefault("Base de Datos", 0L);
        long redisTime = componentTimes.getOrDefault("Redis", 0L);
        long pluginTime = componentTimes.getOrDefault("Plugin Custom", 0L);
        long totalOverhead = componentTimes.getOrDefault("Total Overhead", 0L);

        long totalComponentTime = configTime + dbTime + redisTime + pluginTime;

        sb.append("Timing Breakdown:\n");
        sb.append("  Core Components: ").append(totalComponentTime).append("ms\n");
        sb.append("    ├─ Configuration: ").append(configTime).append("ms\n");
        sb.append("    ├─ Database: ").append(dbTime).append("ms\n");
        sb.append("    ├─ Redis: ").append(redisTime).append("ms\n");
        sb.append("    └─ Plugin Custom: ").append(pluginTime).append("ms\n");
        sb.append("  System Overhead: ").append(totalOverhead).append("ms\n");
        sb.append("  Total: ").append(durationMs).append("ms\n");

        if (durationMs > 0) {
            double componentPercent = (totalComponentTime * 100.0) / durationMs;
            double overheadPercent = (totalOverhead * 100.0) / durationMs;
            sb.append("  Efficiency: ").append(String.format("%.1f%%", componentPercent));
            sb.append(" core work, ").append(String.format("%.1f%%", overheadPercent)).append(" overhead");
        }

        return sb.toString();
    }

    public static void sendReloadResult(org.bukkit.command.CommandSender sender, ReloadResult result) {
        try {
            if (result.isSuccess()) {
                long duration = result.getDurationMs();
                MessageUtils.sendMessage(sender, "{success}✓ Plugin reloaded successfully in {info}" + duration + "ms{success}.");
            } else {
                String error = result.getErrorMessage();
                MessageUtils.sendMessage(sender, "{error}✘ Reload error: " + error + "{error}.");
            }
        } catch (Exception e) {
            DebugUtils.logInternalError("Error processing reload result: " + e.getMessage());
            MessageUtils.sendMessage(sender, "Reload completed but couldn't determine status.");
        }
    }

    public static void sendDetailedReloadResult(org.bukkit.command.CommandSender sender, ReloadResult result) {
        sendReloadResult(sender, result);
        if (Configs.debug()) {
            sender.sendMessage(result.getTimingBreakdown());
        }
    }

    public static void sendStartMessage(org.bukkit.command.CommandSender sender) {
        MessageUtils.sendMessage(sender, "{info}ℹ Reloading...");
    }
}
