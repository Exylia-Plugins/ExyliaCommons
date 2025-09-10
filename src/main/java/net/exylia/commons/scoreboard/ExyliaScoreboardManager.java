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

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Sistema de Scoreboard simplificado y optimizado con configuración avanzada
 * - Configuración única a través de ScoreboardConfig
 * - Soporte completo para ExyliaContext
 * - Configuración avanzada de teams y comportamientos de Minecraft
 * - Optimizado para rendimiento
 * - Estructura limpia con subpackages
 */
public class ExyliaScoreboardManager {

    private final Plugin plugin;
    private final ScoreboardRenderer renderer;
    @Getter
    private final ScoreboardMetrics metrics;
    @Getter
    private final ScoreboardSettings settings;

    // Almacenamiento de scoreboards activos
    private final Map<UUID, PlayerScoreboardInstance> activeScoreboards = new ConcurrentHashMap<>();

    // Backup de scoreboards originales (si está habilitado)
    private final Map<UUID, Scoreboard> originalScoreboards = new ConcurrentHashMap<>();

    // Configuración global
    @Getter
    private boolean enabled = true;
    private long globalUpdateTicks = 20L;

    // Task de actualización global y limpieza
    private BukkitTask updateTask;
    private BukkitTask cleanupTask;

    /**
     * Constructor con configuración por defecto
     */
    public ExyliaScoreboardManager(Plugin plugin) {
        this(plugin, ScoreboardSettings.defaultSettings());
    }

    /**
     * Constructor con configuración personalizada
     */
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

    /**
     * Muestra un scoreboard a un jugador usando configuración
     */
    public ExyliaPlayerScoreboard showScoreboard(Player player, ScoreboardConfig config) {
        return showScoreboard(player, config, ExyliaContext.create());
    }

    /**
     * Muestra un scoreboard con contexto específico
     */
    public ExyliaPlayerScoreboard showScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
        if (!enabled || !player.isOnline() || !config.isEnabled()) {
            return null;
        }

        // Backup del scoreboard original si está habilitado
        if (settings.isBackupOriginalScoreboard() && !originalScoreboards.containsKey(player.getUniqueId())) {
            originalScoreboards.put(player.getUniqueId(), player.getScoreboard());
        }

        // Ocultar scoreboard existente
        hideScoreboard(player);

        // Crear nueva instancia
        PlayerScoreboardInstance instance = new PlayerScoreboardInstance(
                plugin, player, config, context, renderer
        );

        // Almacenar y mostrar
        activeScoreboards.put(player.getUniqueId(), instance);
        instance.show();

        metrics.incrementScoreboardsShown();
        return new ExyliaPlayerScoreboard(instance);
    }

    /**
     * Oculta el scoreboard de un jugador
     */
    public void hideScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.remove(player.getUniqueId());
        if (instance != null) {
            instance.hide();

            // Restaurar scoreboard original si está disponible y configurado
            if (settings.isPreserveOriginalScoreboard()) {
                restoreOriginalScoreboard(player);
            }

            metrics.incrementScoreboardsHidden();
        }
    }

    /**
     * Restaura el scoreboard original del jugador
     */
    private void restoreOriginalScoreboard(Player player) {
        if (player.isOnline()) {
            if (settings.isBackupOriginalScoreboard()) {
                Scoreboard original = originalScoreboards.remove(player.getUniqueId());
                if (original != null) {
                    player.setScoreboard(original);
                    return;
                }
            }

            // Fallback al scoreboard principal
            player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * Obtiene el scoreboard de un jugador
     */
    public ExyliaPlayerScoreboard getScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.get(player.getUniqueId());
        return instance != null ? new ExyliaPlayerScoreboard(instance) : null;
    }

    /**
     * Verifica si un jugador tiene scoreboard activo
     */
    public boolean hasScoreboard(Player player) {
        return activeScoreboards.containsKey(player.getUniqueId());
    }

    // ==================== GESTIÓN DE TEAMS ====================

    /**
     * Añade un jugador al team principal de otro jugador
     * Útil para sistemas de partidos/grupos
     */
    public boolean addPlayerToMainTeam(Player teamOwner, Player playerToAdd) {
        PlayerScoreboardInstance instance = activeScoreboards.get(teamOwner.getUniqueId());
        if (instance == null) return false;

        // Aquí necesitarías acceso al RenderedScoreboard para manipular el team
        // Esta funcionalidad requeriría exponer más métodos en PlayerScoreboardInstance
        logInternalWarn("addPlayerToMainTeam: Funcionalidad pendiente de implementar");
        return false;
    }

    /**
     * Remueve un jugador del team principal
     */
    public boolean removePlayerFromMainTeam(Player teamOwner, Player playerToRemove) {
        logInternalWarn("removePlayerFromMainTeam: Funcionalidad pendiente de implementar");
        return false;
    }

    // ==================== ACTUALIZACIÓN ====================

    /**
     * Actualiza todos los scoreboards activos
     */
    public void updateAll() {
        if (activeScoreboards.isEmpty()) return;

        long startTime = System.nanoTime();
        int updated = 0;
        int errors = 0;

        // Crear snapshot para evitar modificaciones concurrentes
        for (PlayerScoreboardInstance instance : activeScoreboards.values()) {
            try {
                if (instance.getPlayer().isOnline()) {
                    if (instance.shouldUpdate()) {
                        instance.update();
                        updated++;
                    }
                } else {
                    // Jugador desconectado, limpiar
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

    /**
     * Limpia los datos de un jugador desconectado - OPTIMIZADO
     */
    private void cleanupDisconnectedPlayer(Player player) {
        activeScoreboards.remove(player.getUniqueId());
        originalScoreboards.remove(player.getUniqueId());
        
        // Limpiar cache del renderer para este jugador
        renderer.clearPlayerCache(player.getUniqueId().toString());
    }

    /**
     * Task de limpieza automática para teams vacíos
     */
    private void runCleanupCycle() {
        if (!settings.isAutoCleanupEmptyTeams()) return;

        // La limpieza real se hace en el renderer durante las actualizaciones
        // Este task solo registra métricas y logs si es necesario
        int activeCount = activeScoreboards.size();
        if (activeCount == 0) return;

        logInternalDebug("Ciclo de limpieza ejecutado. Scoreboards activos: " + activeCount);
    }

    /**
     * Inicia el task de actualización global
     */
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

    /**
     * Inicia el task de limpieza automática
     */
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

    /**
     * Configura el intervalo de actualización global
     */
    public void setGlobalUpdateTicks(long ticks) {
        this.globalUpdateTicks = Math.max(1L, ticks);
        startUpdateTask();
    }

    /**
     * Habilita o deshabilita el sistema
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hideAllScoreboards();
        }
    }

    /**
     * Oculta todos los scoreboards activos
     */
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

    /**
     * Obtiene estadísticas detalladas del sistema
     */
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

    public static class ScoreboardSystemStats {
        @Getter private final int activeScoreboards;
        @Getter private final int backedUpScoreboards;
        @Getter private final boolean hasCustomTeamSettings;
        @Getter private final boolean autoCleanupEnabled;
        @Getter private final long updateIntervalTicks;

        public ScoreboardSystemStats(int activeScoreboards, int backedUpScoreboards,
                                     boolean hasCustomTeamSettings, boolean autoCleanupEnabled,
                                     long updateIntervalTicks) {
            this.activeScoreboards = activeScoreboards;
            this.backedUpScoreboards = backedUpScoreboards;
            this.hasCustomTeamSettings = hasCustomTeamSettings;
            this.autoCleanupEnabled = autoCleanupEnabled;
            this.updateIntervalTicks = updateIntervalTicks;
        }
    }
}