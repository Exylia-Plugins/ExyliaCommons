// ==================== SCOREBOARD SYSTEM MODERNIZADO ====================

package net.exylia.commons.scoreboard;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

/**
 * Gestor de Scoreboard modernizado que usa el sistema unificado de placeholders
 */
public class ExyliaScoreboardManager {

    private final Plugin plugin;
    private final PlaceholderSystemManager placeholderManager;
    private final Map<String, ScoreboardTemplate> templates;
    private final Map<UUID, PlayerScoreboard> playerScoreboards;

    private BukkitTask globalUpdateTask;

    public ExyliaScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.placeholderManager = PlaceholderSystemManager.getInstance();
        this.templates = new HashMap<>();
        this.playerScoreboards = new ConcurrentHashMap<>();

        startGlobalUpdateTask();
    }

    // ==================== GESTIÓN DE TEMPLATES ====================

    /**
     * Registra un template con contextos personalizados
     */
    public void registerTemplate(String templateId, String title, Map<Integer, String> lines,
                                 int updateTicks, Function<Player, ExyliaContext> contextProvider) {
        ScoreboardTemplate template = new ScoreboardTemplate(templateId, title, lines, updateTicks, contextProvider);
        templates.put(templateId, template);
        logInternalInfo("Template de scoreboard '" + templateId + "' registrado");
    }

    /**
     * Registra un template simple sin contextos dinámicos
     */
    public void registerTemplate(String templateId, String title, Map<Integer, String> lines, int updateTicks) {
        registerTemplate(templateId, title, lines, updateTicks, null);
    }

    /**
     * Establece un proveedor de contexto para un template existente
     */
    public void setTemplateContextProvider(String templateId, Function<Player, ExyliaContext> contextProvider) {
        ScoreboardTemplate template = templates.get(templateId);
        if (template != null) {
            template.setContextProvider(contextProvider);
            logInternalInfo("Contexto establecido para template '" + templateId + "'");
        } else {
            logInternalWarn("Template '" + templateId + "' no encontrado");
        }
    }

    // ==================== MOSTRAR SCOREBOARDS ====================

    /**
     * Muestra un scoreboard a un jugador con contextos específicos
     */
    public PlayerScoreboard showScoreboard(Player player, String templateId, ExyliaContext context) {
        if (!player.isOnline()) {
            throw new IllegalStateException("El jugador no está conectado");
        }

        ScoreboardTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        // Ocultar scoreboard existente
        hideScoreboard(player);

        // Crear y mostrar nuevo scoreboard
        PlayerScoreboard playerScoreboard = new PlayerScoreboard(plugin, player, template, context);
        playerScoreboards.put(player.getUniqueId(), playerScoreboard);
        playerScoreboard.show();

        return playerScoreboard;
    }

    /**
     * Muestra un scoreboard con objetos como contexto
     */
    public PlayerScoreboard showScoreboard(Player player, String templateId, Object... contexts) {
        ExyliaContext context = ExyliaContext.of(contexts);
        return showScoreboard(player, templateId, context);
    }

    /**
     * Muestra un scoreboard usando solo el contexto del template
     */
    public PlayerScoreboard showScoreboard(Player player, String templateId) {
        return showScoreboard(player, templateId, Collections.emptyList());
    }

    // ==================== GESTIÓN DE JUGADORES ====================

    /**
     * Obtiene el scoreboard de un jugador
     */
    public PlayerScoreboard getPlayerScoreboard(Player player) {
        return playerScoreboards.get(player.getUniqueId());
    }

    /**
     * Oculta el scoreboard de un jugador
     */
    public void hideScoreboard(Player player) {
        PlayerScoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.destroy();
        }
    }

    /**
     * Verifica si un jugador tiene scoreboard
     */
    public boolean hasScoreboard(Player player) {
        return playerScoreboards.containsKey(player.getUniqueId());
    }

    // ==================== ACTUALIZACIÓN GLOBAL ====================

    /**
     * Actualiza todos los scoreboards activos
     */
    public void updateAllScoreboards() {
        if (playerScoreboards.isEmpty()) return;

        Map<UUID, PlayerScoreboard> snapshot = new HashMap<>(playerScoreboards);

        for (PlayerScoreboard scoreboard : snapshot.values()) {
            try {
                if (scoreboard.getPlayer().isOnline()) {
                    scoreboard.update();
                } else {
                    hideScoreboard(scoreboard.getPlayer());
                }
            } catch (Exception e) {
                logInternalWarn("Error actualizando scoreboard: " + e.getMessage());
            }
        }
    }

    /**
     * Inicia la tarea global de actualización
     */
    private void startGlobalUpdateTask() {
        if (globalUpdateTask != null) {
            globalUpdateTask.cancel();
        }

        globalUpdateTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::updateAllScoreboards,
                20L,
                5L
        );
    }

    // ==================== CONFIGURACIÓN DESDE ARCHIVO ====================

    /**
     * Carga templates desde configuración
     */
    public void loadTemplatesFromConfig(org.bukkit.configuration.ConfigurationSection config) {
        if (config == null) return;

        templates.clear();

        for (String templateId : config.getKeys(false)) {
            var templateConfig = config.getConfigurationSection(templateId);
            if (templateConfig != null) {
                try {
                    ScoreboardTemplate template = createTemplateFromConfig(templateId, templateConfig);
                    templates.put(templateId, template);
                    logInternalInfo("Template '" + templateId + "' cargado correctamente");
                } catch (Exception e) {
                    logInternalWarn("Error cargando template '" + templateId + "': " + e.getMessage());
                }
            }
        }

        logInternalInfo("Cargados " + templates.size() + " templates de scoreboard");
    }

    /**
     * Crea un template desde configuración
     */
    private ScoreboardTemplate createTemplateFromConfig(String templateId, org.bukkit.configuration.ConfigurationSection config) {
        String title = config.getString("title", "Scoreboard");
        int updateTicks = config.getInt("update-ticks", 20);
        List<String> linesList = config.getStringList("lines");
        Map<Integer, String> lines = new HashMap<>();

        for (int i = 0; i < linesList.size(); i++) {
            lines.put(i, linesList.get(i));
        }

        return new ScoreboardTemplate(templateId, title, lines, updateTicks);
    }

    // ==================== ESTADÍSTICAS Y UTILIDADES ====================

    public int getActiveScoreboardCount() {
        return playerScoreboards.size();
    }

    public int getTemplateCount() {
        return templates.size();
    }

    public void shutdown() {
        if (globalUpdateTask != null) {
            globalUpdateTask.cancel();
        }

        playerScoreboards.values().forEach(PlayerScoreboard::destroy);
        playerScoreboards.clear();
        templates.clear();
    }
}

// ==================== TEMPLATE MODERNIZADO ====================

/**
 * Template de scoreboard modernizado
 */


// ==================== PLAYER SCOREBOARD MODERNIZADO ====================

/**
 * Scoreboard individual modernizado con sistema unificado de placeholders
 */
