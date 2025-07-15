package net.exylia.commons.scoreboard;

import lombok.Getter;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.internal.PlayerScoreboardInstance;
import net.exylia.commons.scoreboard.internal.ScoreboardRenderer;
import net.exylia.commons.scoreboard.metrics.ScoreboardMetrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Sistema de Scoreboard simplificado y optimizado
 * - Configuración única a través de ScoreboardConfig
 * - Soporte completo para ExyliaContext
 * - Optimizado para rendimiento
 * - Estructura limpia con subpackages
 */
public class ExyliaScoreboardManager {

    private final Plugin plugin;
    private final ScoreboardRenderer renderer;
    @Getter
    private final ScoreboardMetrics metrics;

    // Almacenamiento de scoreboards activos
    private final Map<UUID, PlayerScoreboardInstance> activeScoreboards = new ConcurrentHashMap<>();

    // Configuración global
    @Getter
    private boolean enabled = true;
    private long globalUpdateTicks = 20L;

    // Task de actualización global
    private BukkitTask updateTask;

    public ExyliaScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.renderer = new ScoreboardRenderer();
        this.metrics = new ScoreboardMetrics();

        startUpdateTask();
    }

    // ==================== API PRINCIPAL ====================

    /**
     * Muestra un scoreboard a un jugador usando configuración
     */
    public PlayerScoreboard showScoreboard(Player player, ScoreboardConfig config) {
        return showScoreboard(player, config, ExyliaContext.create());
    }

    /**
     * Muestra un scoreboard con contexto específico
     */
    public PlayerScoreboard showScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
        if (!enabled || !player.isOnline() || !config.isEnabled()) {
            return null;
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
        return new PlayerScoreboard(instance);
    }

    /**
     * Oculta el scoreboard de un jugador
     */
    public void hideScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.remove(player.getUniqueId());
        if (instance != null) {
            instance.hide();
            metrics.incrementScoreboardsHidden();
        }
    }

    /**
     * Obtiene el scoreboard de un jugador
     */
    public PlayerScoreboard getScoreboard(Player player) {
        PlayerScoreboardInstance instance = activeScoreboards.get(player.getUniqueId());
        return instance != null ? new PlayerScoreboard(instance) : null;
    }

    /**
     * Verifica si un jugador tiene scoreboard activo
     */
    public boolean hasScoreboard(Player player) {
        return activeScoreboards.containsKey(player.getUniqueId());
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
                    activeScoreboards.remove(instance.getPlayer().getUniqueId());
                }
            } catch (Exception e) {
                errors++;
                logInternalWarn("Error actualizando scoreboard de " + instance.getPlayer().getName() + ": " + e.getMessage());
            }
        }

        long duration = System.nanoTime() - startTime;
        metrics.recordUpdateCycle(updated, errors, duration);
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
        activeScoreboards.values().forEach(PlayerScoreboardInstance::hide);
        activeScoreboards.clear();
    }

    // ==================== INFORMACIÓN Y MÉTRICAS ====================

    public int getActiveScoreboardCount() {
        return activeScoreboards.size();
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        hideAllScoreboards();
    }
}

// ==================== WRAPPER PÚBLICO ====================

/**
 * Wrapper público para acceso controlado a scoreboard de jugador
 */
class PlayerScoreboard {

    private final PlayerScoreboardInstance instance;

    PlayerScoreboard(PlayerScoreboardInstance instance) {
        this.instance = instance;
    }

    /**
     * Actualiza el contexto del scoreboard
     */
    public PlayerScoreboard updateContext(ExyliaContext newContext) {
        instance.updateContext(newContext);
        return this;
    }

    /**
     * Añade objetos al contexto
     */
    public PlayerScoreboard addToContext(Object... objects) {
        instance.addToContext(objects);
        return this;
    }

    /**
     * Fuerza una actualización inmediata
     */
    public PlayerScoreboard forceUpdate() {
        instance.update();
        return this;
    }

    /**
     * Obtiene el jugador propietario
     */
    public Player getPlayer() {
        return instance.getPlayer();
    }

    /**
     * Verifica si está visible
     */
    public boolean isVisible() {
        return instance.isVisible();
    }

    /**
     * Obtiene la configuración utilizada
     */
    public ScoreboardConfig getConfig() {
        return instance.getConfig();
    }
}