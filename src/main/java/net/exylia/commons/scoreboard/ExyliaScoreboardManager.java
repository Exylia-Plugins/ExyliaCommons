package net.exylia.commons.scoreboard;

import lombok.Getter;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.config.ScoreboardSettings;
import net.exylia.commons.scoreboard.internal.PlayerScoreboardInstance;
import net.exylia.commons.scoreboard.internal.ScoreboardRenderer;
import net.exylia.commons.scoreboard.metrics.ScoreboardMetrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.DebugUtils.*;

@Getter
public class ExyliaScoreboardManager {

    private final Plugin plugin;
    private final ScoreboardRenderer renderer;
    private final ScoreboardMetrics metrics;
    private final ScoreboardSettings settings;
    private final Map<UUID, PlayerScoreboardInstance> activeScoreboards = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> originalScoreboards = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private long globalUpdateTicks = 20L;
    private BukkitTask updateTask;
    private BukkitTask cleanupTask;

    public ExyliaScoreboardManager(Plugin plugin) {
        this(plugin, ScoreboardSettings.defaultSettings());
    }

    public ExyliaScoreboardManager(Plugin plugin, ScoreboardSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.renderer = new ScoreboardRenderer(settings);
        this.metrics = new ScoreboardMetrics();

        startUpdateTask();
        if (settings.isAutoCleanupEmptyTeams()) {
            startCleanupTask();
        }
    }

    // ==================== API PRINCIPAL ====================

    public ExyliaPlayerScoreboard showScoreboard(Player player, ScoreboardConfig config) {
        return showScoreboard(player, config, ExyliaContext.create());
    }

    public ExyliaPlayerScoreboard showScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
        if (!enabled || !player.isOnline() || !config.isEnabled()) {
            return null;
        }
        if (settings.isBackupOriginalScoreboard() && !originalScoreboards.containsKey(player.getUniqueId())) {
            originalScoreboards.put(player.getUniqueId(), player.getScoreboard());
        }
        hideScoreboard(player);
        PlayerScoreboardInstance instance = new PlayerScoreboardInstance(
                plugin, player, config, context, renderer
        );
        activeScoreboards.put(player.getUniqueId(), instance);
        instance.show();

        metrics.incrementScoreboardsShown();
        return new ExyliaPlayerScoreboard(instance);
    }

    public void hideScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.remove(player.getUniqueId());
        if (instance != null) {
            instance.hide();
            if (settings.isPreserveOriginalScoreboard()) {
                restoreOriginalScoreboard(player);
            }

            metrics.incrementScoreboardsHidden();
        }
    }

    private void restoreOriginalScoreboard(Player player) {
        if (player.isOnline()) {
            if (settings.isBackupOriginalScoreboard()) {
                Scoreboard original = originalScoreboards.remove(player.getUniqueId());
                if (original != null) {
                    player.setScoreboard(original);
                    return;
                }
            }

            player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
    }

    public ExyliaPlayerScoreboard getScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.get(player.getUniqueId());
        return instance != null ? new ExyliaPlayerScoreboard(instance) : null;
    }

    public boolean hasScoreboard(Player player) {
        return activeScoreboards.containsKey(player.getUniqueId());
    }

    // ==================== GESTIÓN DE TEAMS ====================
    public boolean addPlayerToMainTeam(Player teamOwner, Player playerToAdd) {
        PlayerScoreboardInstance instance = activeScoreboards.get(teamOwner.getUniqueId());
        if (instance == null) return false;
        logInternalWarn("addPlayerToMainTeam: Funcionalidad pendiente de implementar");
        return false;
    }

    public boolean removePlayerFromMainTeam(Player teamOwner, Player playerToRemove) {
        logInternalWarn("removePlayerFromMainTeam: Funcionalidad pendiente de implementar");
        return false;
    }

    // ==================== ACTUALIZACIÓN ====================

    public void updateAll() {
        if (activeScoreboards.isEmpty()) return;

        long startTime = System.nanoTime();
        int updated = 0;
        int errors = 0;

        for (PlayerScoreboardInstance instance : activeScoreboards.values()) {
            try {
                if (instance.getPlayer().isOnline()) {
                    if (instance.shouldUpdate()) {
                        instance.update();
                        updated++;
                    }
                } else {
                    cleanupDisconnectedPlayer(instance.getPlayer());
                }
            } catch (Exception e) {
                errors++;
                logInternalDebug("Error actualizando scoreboard de " + instance.getPlayer().getName() + ": " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        metrics.recordUpdateCycle(updated, errors, duration);
    }

    private void cleanupDisconnectedPlayer(Player player) {
        activeScoreboards.remove(player.getUniqueId());
        originalScoreboards.remove(player.getUniqueId());
        renderer.clearPlayerCache(player.getUniqueId().toString());
    }

    private void runCleanupCycle() {
        if (!settings.isAutoCleanupEmptyTeams()) return;
        int activeCount = activeScoreboards.size();
        if (activeCount == 0) return;

        logInternalDebug("Ciclo de limpieza ejecutado. Scoreboards activos: " + activeCount);
    }
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::updateAll,
                globalUpdateTicks,
                globalUpdateTicks
        );
    }
    private void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::runCleanupCycle,
                settings.getCleanupInterval(),
                settings.getCleanupInterval()
        );
    }

    // ==================== CONFIGURACIÓN ====================
    public void setGlobalUpdateTicks(long ticks) {
        this.globalUpdateTicks = Math.max(1L, ticks);
        startUpdateTask();
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hideAllScoreboards();
        }
    }
    public void hideAllScoreboards() {
        activeScoreboards.values().forEach(instance -> {
            instance.hide();
            if (settings.isPreserveOriginalScoreboard()) {
                restoreOriginalScoreboard(instance.getPlayer());
            }
        });
        activeScoreboards.clear();
        originalScoreboards.clear();
    }

    // ==================== INFORMACIÓN Y MÉTRICAS ====================

    public int getActiveScoreboardCount() {
        return activeScoreboards.size();
    }
    public ScoreboardSystemStats getSystemStats() {
        return new ScoreboardSystemStats(
                activeScoreboards.size(),
                originalScoreboards.size(),
                settings.hasCustomTeamSettings(),
                settings.isAutoCleanupEmptyTeams(),
                globalUpdateTicks
        );
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        hideAllScoreboards();
    }

    // ==================== CLASE DE ESTADÍSTICAS ====================
        public record ScoreboardSystemStats(int activeScoreboards, int backedUpScoreboards, boolean hasCustomTeamSettings,
                                            boolean autoCleanupEnabled, long updateIntervalTicks) {
    }
}