package net.exylia.commons.v2.reload.api;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.reload.core.ReloadManagerV2;
import net.exylia.commons.v2.reload.detector.SystemAvailability;
import net.exylia.commons.v2.reload.stats.ReloadStats;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.exylia.commons.utils.DebugUtils.logInternalError;

@Getter
public class ReloadAPI {
    private static ReloadAPI instance;
    private final ReloadManagerV2 manager;

    private ReloadAPI(ExyliaPlugin plugin) {
        this.manager = new ReloadManagerV2(plugin);
    }

    public static ReloadAPI initialize(ExyliaPlugin plugin) {
        if (instance == null) {
            instance = new ReloadAPI(plugin);
        }
        return instance;
    }

    public static ReloadAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ReloadAPI not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public CompletableFuture<ReloadStats> reloadAll() {
        return manager.executeReloadAll(null, Collections.emptySet());
    }

    public CompletableFuture<ReloadStats> reloadAll(Player player) {
        return manager.executeReloadAll(player, Collections.emptySet());
    }

    public CompletableFuture<ReloadStats> reloadAll(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return manager.executeReloadAll(player, Collections.emptySet())
                .thenApply(stats -> {
                    if (!(sender instanceof Player)) {
                        SchedulerManager.getInstance().runTask(() ->
                                sendDetailedStats(sender, stats));
                    }
                    return stats;
                });
    }

    public CompletableFuture<ReloadStats> reloadAllExcept(String... excludedSystems) {
        return manager.executeReloadAll(null, new HashSet<>(Arrays.asList(excludedSystems)));
    }

    public CompletableFuture<ReloadStats> reloadAllExcept(Player player, String... excludedSystems) {
        return manager.executeReloadAll(player, new HashSet<>(Arrays.asList(excludedSystems)));
    }

    public CompletableFuture<ReloadStats> reloadAllExcept(CommandSender sender, String... excludedSystems) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return manager.executeReloadAll(player, new HashSet<>(Arrays.asList(excludedSystems)))
                .thenApply(stats -> {
                    if (!(sender instanceof Player)) {
                        SchedulerManager.getInstance().runTask(() ->
                                sendDetailedStats(sender, stats));
                    }
                    return stats;
                });
    }

    public CompletableFuture<ReloadStats> reloadAll(long timeoutSeconds) {
        return reloadAll()
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logInternalError("Reload cancelled by timeout (" + timeoutSeconds + "s)");
                    } else {
                        logInternalError("Error in reload with timeout: " + throwable.getMessage());
                    }
                    return null;
                });
    }

    public CompletableFuture<ReloadStats> reloadAll(Player player, long timeoutSeconds) {
        return reloadAll(player)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logInternalError("Reload cancelled by timeout (" + timeoutSeconds + "s)");
                    } else {
                        logInternalError("Error in reload with timeout: " + throwable.getMessage());
                    }
                    return null;
                });
    }

    public CompletableFuture<ReloadStats> reloadAll(CommandSender sender, long timeoutSeconds) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return manager.executeReloadAll(player)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .thenApply(stats -> {
                    if (!(sender instanceof Player)) {
                        SchedulerManager.getInstance().runTask(() ->
                                sendDetailedStats(sender, stats));
                    }
                    return stats;
                })
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logInternalError("Reload cancelled by timeout (" + timeoutSeconds + "s)");
                        sender.sendMessage("§c✖ Reload cancelled: Timeout after " + timeoutSeconds + " seconds");
                    } else {
                        logInternalError("Error in reload: " + throwable.getMessage());
                        sender.sendMessage("§c✖ Reload error: " + throwable.getMessage());
                    }
                    return null;
                });
    }

    public CompletableFuture<ReloadStats> reloadSystem(String systemName) {
        return manager.executeReloadSystem(systemName);
    }

    public void registerReloadable(String name, ReloadableSystem system) {
        manager.registerSystem(name, system);
    }

    public void unregisterReloadable(String name) {
        manager.unregisterSystem(name);
    }

    public SystemAvailability getAvailability() {
        return manager.detectSystems();
    }

    public List<String> getRegisteredSystems() {
        return new ArrayList<>(manager.getSystems().keySet());
    }

    public List<String> getAvailableSystems() {
        return new ArrayList<>(manager.detectSystems().getAvailableSystems());
    }

    private void sendDetailedStats(CommandSender sender, ReloadStats stats) {
        if (stats == null) {
            sender.sendMessage("§c✖ Reload failed or timed out");
            return;
        }

        String statusIcon = stats.isSuccess() ? "§a✔" : "§c✖";
        String statusText = stats.isSuccess() ? "§aSuccess" : "§cFailed";

        sender.sendMessage("");
        sender.sendMessage("§6⚡ §eReload System V2 §7- " + statusIcon + " " + statusText);
        sender.sendMessage("§7Total Duration: §e" + stats.getFormattedDuration());
        sender.sendMessage("§7Systems: §a" + stats.getSuccessCount() + " success §8| " +
                "§c" + stats.getFailureCount() + " failed §8| " +
                "§8" + stats.getSkippedCount() + " skipped");

        if (!stats.isSuccess() && !stats.getErrors().isEmpty()) {
            sender.sendMessage("§cErrors:");
            stats.getErrors().forEach((name, error) ->
                    sender.sendMessage("§c  - " + name + ": §7" + error.getMessage())
            );
        }

        sender.sendMessage("");
    }
}
