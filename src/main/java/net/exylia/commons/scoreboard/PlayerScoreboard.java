package net.exylia.commons.scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard individual con soporte de contexto personalizado
 */
public class PlayerScoreboard {

    private final Plugin plugin;
    private final Player player;
    private final ScoreboardTemplate template;
    private final ConfigManager configManager;
    private final boolean placeholderAPIEnabled;
    private final Scoreboard scoreboard;
    private final Objective objective;

    // Cache para equipos y contenido previo
    private final Map<Integer, Team> teamCache = new HashMap<>();
    private final Map<Integer, String> lastContent = new HashMap<>();

    private boolean visible = false;
    private long lastUpdate = 0;
    private Object cachedContext = null; // Cache del contexto
    private long lastContextUpdate = 0; // Tiempo de última actualización del contexto

    private static final ScoreboardManager SCOREBOARD_MANAGER = Bukkit.getScoreboardManager();
    private static final String OBJECTIVE_NAME = "exylia";

    public PlayerScoreboard(Plugin plugin, Player player, ScoreboardTemplate template,
                            ConfigManager configManager, boolean placeholderAPIEnabled) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.configManager = configManager;
        this.placeholderAPIEnabled = placeholderAPIEnabled;

        this.scoreboard = SCOREBOARD_MANAGER.getNewScoreboard();

        // Procesar título con colores, placeholders y contexto
        String processedTitle = processText(template.getTitle());
        Component titleComponent = ColorUtils.parse(processedTitle);

        this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", titleComponent);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Obtiene el contexto actual para el jugador
     */
    private Object getCurrentContext() {
        // Actualizar contexto cada segundo como máximo
        long currentTime = System.currentTimeMillis();
        if (cachedContext == null || currentTime - lastContextUpdate > 1000) {
            cachedContext = template.getContext(player);
            lastContextUpdate = currentTime;
        }
        return cachedContext;
    }

    /**
     * Procesa texto aplicando colores predefinidos, placeholders custom con contexto y PlaceholderAPI
     */
    private String processText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String processed = ColorUtils.applyColorPresets(text);
        Object context = getCurrentContext();
        processed = PlaceholderRegistry.process(processed, context, player);
        if (placeholderAPIEnabled) {
            try {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            } catch (Exception e) {
            }
        }

        return processed;
    }

    public PlayerScoreboard show() {
        if (visible || !player.isOnline()) return this;

        visible = true;
        update();
        player.setScoreboard(scoreboard);
        return this;
    }

    public PlayerScoreboard hide() {
        if (!visible) return this;

        visible = false;
        if (player.isOnline()) {
            player.setScoreboard(SCOREBOARD_MANAGER.getMainScoreboard());
        }
        return this;
    }

    public PlayerScoreboard update() {
        if (!visible || !player.isOnline()) {
            destroy();
            return this;
        }

        // Verificar si debe actualizarse según el tiempo configurado
        long currentTime = System.currentTimeMillis();
        if (template.shouldUpdate()) {
            long updateInterval = template.getUpdateTicks() * 50; // Convertir ticks a ms
            if (currentTime - lastUpdate < updateInterval) {
                return this; // No es momento de actualizar aún
            }
        }

        lastUpdate = currentTime;

        try {
            // Actualizar título con contexto
            String processedTitle = processText(template.getTitle());
            Component titleComponent = ColorUtils.parse(processedTitle);
            objective.displayName(titleComponent);

            // Actualizar líneas con contexto
            Map<Integer, String> lines = template.getLines();
            for (Map.Entry<Integer, String> entry : lines.entrySet()) {
                int position = entry.getKey();
                String lineText = entry.getValue();

                updateLine(position, lineText);
            }

            // Limpiar líneas no usadas
            cleanupUnusedLines(lines.keySet());

        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando scoreboard para " + player.getName() + ": " + e.getMessage());
        }

        return this;
    }

    private void updateLine(int position, String lineText) {
        // Procesar texto con colores, placeholders y contexto
        String processedText = processText(lineText);

        // Verificar si el contenido cambió
        String previousContent = lastContent.get(position);
        if (processedText.equals(previousContent)) {
            return; // Sin cambios
        }

        // Actualizar cache
        lastContent.put(position, processedText);

        // Obtener o crear equipo
        Team team = teamCache.computeIfAbsent(position, pos -> {
            String teamName = "line" + pos;
            Team newTeam = scoreboard.getTeam(teamName);
            if (newTeam == null) {
                newTeam = scoreboard.registerNewTeam(teamName);
            }
            return newTeam;
        });

        // Aplicar contenido al equipo
        Component lineComponent = ColorUtils.parse(processedText);
        team.prefix(lineComponent);

        String entryName = getUniqueEntryName(position);
        if (!team.hasEntry(entryName)) {
            team.addEntry(entryName);
        }

        // Configurar score (líneas más arriba tienen score más alto)
        Score score = objective.getScore(entryName);
        score.setScore(template.getLines().size() - position);
    }

    private void cleanupUnusedLines(java.util.Set<Integer> activeLines) {
        teamCache.entrySet().removeIf(entry -> {
            int position = entry.getKey();
            if (!activeLines.contains(position)) {
                Team team = entry.getValue();
                try {
                    String entryName = getUniqueEntryName(position);
                    scoreboard.resetScores(entryName);
                    team.unregister();
                } catch (Exception ignored) {}
                lastContent.remove(position);
                return true;
            }
            return false;
        });
    }

    public void destroy() {
        hide();

        try {
            // Limpiar equipos
            for (Team team : teamCache.values()) {
                team.unregister();
            }
            teamCache.clear();
            lastContent.clear();

            // Limpiar objetivo
            objective.unregister();
        } catch (Exception ignored) {}
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerUUID() {
        return player.getUniqueId();
    }

    public ScoreboardTemplate getTemplate() {
        return template;
    }

    public boolean isVisible() {
        return visible;
    }

    private String getUniqueEntryName(int line) {
        char[] colors = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        return "§" + colors[line % colors.length] + "§r";
    }
}