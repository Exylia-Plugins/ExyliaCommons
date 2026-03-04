package net.exylia.commons.v2.reload.core;
import net.exylia.commons.v2.debug.api.DebugAPI;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.loader.ExyliaLoaderPlugin;
import net.exylia.commons.v2.reload.adapter.*;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.reload.api.ReloadableSystem;
import net.exylia.commons.v2.reload.detector.SystemAvailability;
import net.exylia.commons.v2.reload.stats.ReloadStats;
import net.exylia.commons.v2.reload.stats.SystemReloadMetrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Getter
public class ReloadManagerV2 {
    private final JavaPlugin plugin;
    private final SystemDetector detector;
    private final ReloadOrchestrator orchestrator;
    private final Map<String, ReloadableSystem> systems;

    public ReloadManagerV2(JavaPlugin plugin) {
        this.plugin = plugin;
        this.detector = new SystemDetector();
        this.orchestrator = new ReloadOrchestrator();
        this.systems = new LinkedHashMap<>();

        registerDefaultSystems();
    }

    private void registerDefaultSystems() {
        registerSystem("Config", new ConfigAdapter());
        registerSystem("ConfigSchema", new ConfigSchemaAdapter());
        registerSystem("ConfigSystem", new ConfigSystemAdapter());
        registerSystem("DebugConfig", new DebugConfigAdapter());
        registerSystem("Messages", new MessagesAdapter());
        registerSystem("DatabaseV2", new DatabaseV2Adapter());
        registerSystem("Redis", new RedisAdapter(plugin));
        registerSystem("ClanManager", new ClanAdapter());
        registerSystem("ScoreboardManager", new ScoreboardAdapter());
        registerSystem("HologramManager", new HologramAdapter());
        registerSystem("ActionManager", new ActionAdapter());
        registerSystem("RegionManager", new RegionAdapter());
        registerSystem("PlaceholderSystem", new PlaceholderAdapter());
        registerSystem("CommandManager", new CommandAdapter());
        registerSystem("RewardManager", new RewardAdapter());
        registerSystem("SkullManager", new SkullAdapter());
        registerSystem("VisualManager", new VisualAdapter());
        registerSystem("ColorPresetManager", new ColorPresetAdapter());
        registerSystem("FormatterRegistry", new FormatterAdapter());
        registerSystem("ColorSystem", new ColorAdapter());
        registerSystem("DiscordWebhooks", new DiscordAdapter());
    }

    public CompletableFuture<ReloadStats> executeReloadAll() {
        return executeReloadAll(null, Collections.emptySet());
    }

    public CompletableFuture<ReloadStats> executeReloadAll(org.bukkit.entity.Player player) {
        return executeReloadAll(player, Collections.emptySet());
    }

    public CompletableFuture<ReloadStats> executeReloadAll(org.bukkit.entity.Player player, Set<String> excludedSystems) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            DebugAPI.logLibInfo("=== Starting Complete Reload ===");

            if (!excludedSystems.isEmpty()) {
                DebugAPI.logLibInfo("Excluding systems: " + String.join(", ", excludedSystems));
            }

            if (player != null) {
                net.exylia.commons.v2.reload.notification.ReloadNotifier.notifyStart(player);
            }

            ReloadStats.Builder statsBuilder = new ReloadStats.Builder();
            ReloadContext context = new ReloadContext(plugin);

            SystemAvailability availability = detector.detectAll(systems);

            List<ReloadableSystem> sortedSystems = getSortedSystems();
            int totalSystems = (int) sortedSystems.stream()
                    .filter(s -> !excludedSystems.contains(s.getName()))
                    .count();
            int currentSystem = 0;

            for (ReloadableSystem system : sortedSystems) {
                if (excludedSystems.contains(system.getName())) {
                    DebugAPI.logLibDebug("Skipping " + system.getName() + ": Excluded by request");
                    statsBuilder.skip(system.getName(), "Excluded by request");
                    continue;
                }

                if (!availability.isAvailable(system.getName())) {
                    String reason = availability.getReason(system.getName());
                    DebugAPI.logLibDebug("Skipping " + system.getName() + ": " + reason);
                    statsBuilder.skip(system.getName(), reason);
                    continue;
                }

                currentSystem++;
                DebugAPI.logLibDebug("Reloading " + system.getName() + "...");

                if (player != null) {
                    net.exylia.commons.v2.reload.notification.ReloadNotifier.notifySystemStart(player, system.getName());
                    net.exylia.commons.v2.reload.notification.ReloadNotifier.notifyProgress(player, currentSystem, totalSystems, system.getName());
                }

                try {
                    SystemReloadMetrics metrics = orchestrator
                            .executeReload(system, context)
                            .get();

                    if (metrics.isSuccess()) {
                        context.markReloaded(system.getName());
                        DebugAPI.logLibInfo(system.getName() + " reloaded in " + metrics.getFormattedDuration());
                    } else {
                        DebugAPI.logLibError(system.getName() + " reload failed: " + metrics.getPhase());
                    }

                    statsBuilder.add(system.getName(), metrics);

                    if (player != null) {
                        net.exylia.commons.v2.reload.notification.ReloadNotifier.notifySystemComplete(player, metrics);
                    }

                    if (!metrics.isSuccess() && system.isCritical()) {
                        DebugAPI.logLibError("Critical system " + system.getName() + " failed. Stopping reload.");
                        break;
                    }

                } catch (Exception e) {
                    DebugAPI.logLibError("Exception during " + system.getName() + " reload: " + e.getMessage());
                    statsBuilder.addError(system.getName(), e);

                    if (system.isCritical()) {
                        DebugAPI.logLibError("Critical system " + system.getName() + " failed. Stopping reload.");
                        break;
                    }
                }
            }

            callPluginHooks(context);

            long totalDuration = System.currentTimeMillis() - startTime;
            statsBuilder.setTotalDuration(totalDuration);

            ReloadStats stats = statsBuilder.build();
            context.setPartialStats(stats);

            if (player != null) {
                net.exylia.commons.v2.reload.notification.ReloadNotifier.notifyComplete(player, stats);
            }

            if (stats.isSuccess()) {
                DebugAPI.logLibInfo("=== RELOAD COMPLETED: " + stats.getSuccessCount() + " systems in " + stats.getFormattedDuration() + " ===");
            } else {
                DebugAPI.logLibError("=== RELOAD COMPLETED WITH ERRORS: " + stats.getFailureCount() + " failures ===");
            }

            return stats;
        });
    }

    public CompletableFuture<ReloadStats> executeReloadSystem(String systemName) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            ReloadStats.Builder statsBuilder = new ReloadStats.Builder();
            ReloadContext context = new ReloadContext(plugin);

            ReloadableSystem system = systems.get(systemName);
            if (system == null) {
                statsBuilder.addError(systemName, new Exception("System not found"));
                statsBuilder.setTotalDuration(System.currentTimeMillis() - startTime);
                return statsBuilder.build();
            }

            SystemAvailability availability = detector.detectAll(systems);
            if (!availability.isAvailable(systemName)) {
                String reason = availability.getReason(systemName);
                statsBuilder.skip(systemName, reason);
                statsBuilder.setTotalDuration(System.currentTimeMillis() - startTime);
                return statsBuilder.build();
            }

            try {
                SystemReloadMetrics metrics = orchestrator
                        .executeReload(system, context)
                        .get();

                if (metrics.isSuccess()) {
                    context.markReloaded(systemName);
                }

                statsBuilder.add(systemName, metrics);
            } catch (Exception e) {
                statsBuilder.addError(systemName, e);
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            statsBuilder.setTotalDuration(totalDuration);

            return statsBuilder.build();
        });
    }

    private void callPluginHooks(ReloadContext context) {
        try {
            Tasks.sync(() -> {
                ExyliaPlugin instance = ExyliaPlugin.getInstance();
                if (instance != null) {
                    instance.callOnReload(context);
                }
                ExyliaLoaderPlugin.callOnReloadForActivePlugins(context);
            });
        } catch (Exception e) {
            DebugAPI.logLibError("Error calling plugin hooks: " + e.getMessage());
        }
    }

    public void registerSystem(String name, ReloadableSystem system) {
        systems.put(name, system);
    }

    public void unregisterSystem(String name) {
        systems.remove(name);
    }

    public SystemAvailability detectSystems() {
        return detector.detectAll(systems);
    }

    private List<ReloadableSystem> getSortedSystems() {
        return systems.values().stream()
                .sorted(Comparator.comparingInt(s -> s.getPriority().getOrder()))
                .collect(Collectors.toList());
    }
}
