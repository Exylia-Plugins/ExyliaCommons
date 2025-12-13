package net.exylia.commons.v2.reload.notification;

import net.exylia.commons.v2.reload.stats.ReloadStats;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.entity.Player;

public class ReloadNotifier {

    public static void notifyStart(Player player) {
        if (player == null) return;
        MessageAPI.send(player, " ");
        MessageAPI.send(player, "{warning}⚡ {info}Reload {letters}- &fStarting...");
    }

    public static void notifySystemStart(Player player, String systemName) {
    }

    public static void notifySystemComplete(Player player, SystemReloadMetrics metrics) {
        if (player == null) return;

        String status = metrics.isSuccess() ? "{success}✔" : "{error}✖";
        String duration = metrics.getFormattedDuration();
        MessageAPI.send(player, status + " {letters}" + metrics.getSystemName() + " {letters_black}(" + duration + ")");
    }

    public static void notifyComplete(Player player, ReloadStats stats) {
        if (player == null) return;

        String statusIcon = stats.isSuccess() ? "{success}✔" : "{error}✖";
        String statusText = stats.isSuccess() ? "{success}Success" : "{error}Failed";

        MessageAPI.send(player, "");
        MessageAPI.send(player, "{warning}⚡ {info}Reload {letters}- " + statusIcon + " " + statusText);
        MessageAPI.send(player, "{letters}Total Duration: {info}" + stats.getFormattedDuration());
        MessageAPI.send(player, "{letters}Systems: {success}" + stats.getSuccessCount() + " success {letters_black}| " +
                "{error}" + stats.getFailureCount() + " failed {letters_black}| " +
                "{letters_black}" + stats.getSkippedCount() + " skipped");

        if (!stats.isSuccess() && !stats.getErrors().isEmpty()) {
            MessageAPI.send(player, "{error}Errors:");
            stats.getErrors().forEach((name, error) ->
                    MessageAPI.send(player, "{error}  - " + name + ": {letters}" + error.getMessage())
            );
        }

        MessageAPI.send(player, " ");
    }

    public static void notifyProgress(Player player, int current, int total, String currentSystem) {
    }
}
