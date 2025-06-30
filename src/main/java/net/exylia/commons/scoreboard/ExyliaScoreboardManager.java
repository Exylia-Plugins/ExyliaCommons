package net.exylia.commons.scoreboard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.exylia.commons.utils.DebugUtils.logInfo;
import static net.exylia.commons.utils.DebugUtils.logWarn;

public class ExyliaScoreboardManager {

    private final Plugin plugin;
    private final Map<String, ScoreboardTemplate> templates;
    private final Map<UUID, PlayerScoreboard> playerScoreboards;
    private final boolean placeholderAPIEnabled;

    private BukkitTask globalUpdateTask;

    public ExyliaScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        this.templates = new HashMap<>();
        this.playerScoreboards = new ConcurrentHashMap<>();
        this.placeholderAPIEnabled = checkPlaceholderAPI();

        if (placeholderAPIEnabled) {
            logInfo("PlaceholderAPI encontrado, habilitando soporte de placeholders.");
        }

        startGlobalUpdateTask();
    }

    private boolean checkPlaceholderAPI() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Registra un template con un proveedor de contexto personalizado
     */
    public void registerTemplate(String templateId, String title, Map<Integer, String> lines,
                                 int updateTicks, Function<Player, Object> contextProvider) {
        ScoreboardTemplate template = new ScoreboardTemplate(templateId, title, lines, updateTicks, contextProvider);
        templates.put(templateId, template);
        logInfo("Template '" + templateId + "' registrado con contexto personalizado");
    }

    /**
     * Establece un proveedor de contexto para un template existente
     */
    public void setTemplateContext(String templateId, Function<Player, Object> contextProvider) {
        ScoreboardTemplate template = templates.get(templateId);
        if (template != null) {
            template.setContextProvider(contextProvider);
            logInfo("Contexto establecido para template '" + templateId + "'");
        } else {
            logWarn("Template '" + templateId + "' no encontrado para establecer contexto");
        }
    }

    /**
     * Muestra un scoreboard a un jugador con contexto personalizado
     */
    public PlayerScoreboard showScoreboard(Player player, String templateId, Function<Player, Object> contextProvider) {
        if (!player.isOnline()) {
            throw new IllegalStateException("El jugador no está conectado");
        }

        ScoreboardTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template no encontrado: " + templateId);
        }

        // Crear una copia del template con el contexto específico
        ScoreboardTemplate contextTemplate = new ScoreboardTemplate(
                template.getId(),
                template.getTitle(),
                template.getLines(),
                template.getUpdateTicks(),
                contextProvider
        );

        // Ocultar scoreboard existente
        hideScoreboard(player);

        // Crear y mostrar nuevo scoreboard
        PlayerScoreboard playerScoreboard = new PlayerScoreboard(
                plugin, player, contextTemplate, placeholderAPIEnabled
        );
        playerScoreboards.put(player.getUniqueId(), playerScoreboard);
        playerScoreboard.show();

        return playerScoreboard;
    }

    public PlayerScoreboard showScoreboard(Player player, String templateId) {
        return showScoreboard(player, templateId, null);
    }

    public void loadTemplatesFromConfig(ConfigurationSection config) {
        if (config == null) return;

        templates.clear();

        for (String templateId : config.getKeys(false)) {
            ConfigurationSection templateConfig = config.getConfigurationSection(templateId);
            if (templateConfig != null) {
                try {
                    ScoreboardTemplate template = createTemplateFromConfig(templateId, templateConfig);
                    templates.put(templateId, template);
                    logInfo("Template '" + templateId + "' cargado correctamente");
                } catch (Exception e) {
                    logWarn("Error cargando template '" + templateId + "': " + e.getMessage());
                }
            }
        }

        logInfo("Cargados " + templates.size() + " templates de scoreboard");
    }

    private ScoreboardTemplate createTemplateFromConfig(String templateId, ConfigurationSection config) {
        String title = config.getString("title", "Scoreboard");
        int updateTicks = config.getInt("update-ticks", 20);
        List<String> linesList = config.getStringList("lines");
        Map<Integer, String> lines = new HashMap<>();

        for (int i = 0; i < linesList.size(); i++) {
            lines.put(i, linesList.get(i));
        }

        return new ScoreboardTemplate(templateId, title, lines, updateTicks);
    }

    public PlayerScoreboard getPlayerScoreboard(Player player) {
        return playerScoreboards.get(player.getUniqueId());
    }

    public void hideScoreboard(Player player) {
        PlayerScoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.destroy();
        }
    }

    public boolean hasScoreboard(Player player) {
        return playerScoreboards.containsKey(player.getUniqueId());
    }

    /**
     * Actualiza todos los scoreboards activos
     */
    public void updateAllScoreboards() {
        if (playerScoreboards.isEmpty()) return;

        // Crear snapshot para evitar problemas de concurrencia
        Map<UUID, PlayerScoreboard> snapshot = new HashMap<>(playerScoreboards);

        for (PlayerScoreboard scoreboard : snapshot.values()) {
            try {
                if (scoreboard.getPlayer().isOnline()) {
                    scoreboard.update();
                } else {
                    // Limpiar scoreboards de jugadores desconectados
                    hideScoreboard(scoreboard.getPlayer());
                }
            } catch (Exception e) {
                logWarn("Error actualizando scoreboard: " + e.getMessage());
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

    Plugin getPlugin() {
        return plugin;
    }
}