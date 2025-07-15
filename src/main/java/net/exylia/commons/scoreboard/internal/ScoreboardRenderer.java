package net.exylia.commons.scoreboard.internal;

import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Renderizador optimizado de scoreboards
 * Maneja la creación y actualización eficiente de scoreboards de Bukkit
 */
public class ScoreboardRenderer {

    private static final ScoreboardManager BUKKIT_SCOREBOARD_MANAGER = Bukkit.getScoreboardManager();
    private static final String OBJECTIVE_NAME = "exylia_sb";

    // Cache para equipos reutilizables
    private final Map<String, String> entryCache = new HashMap<>();

    /**
     * Crea un nuevo scoreboard para un jugador
     */
    public RenderedScoreboard createScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
        Scoreboard bukkit = BUKKIT_SCOREBOARD_MANAGER.getNewScoreboard();

        // Procesar título
        String processedTitle = context.processPlaceholders(config.getTitle(), player);
        Component titleComponent = ColorUtils.parse(processedTitle);

        // Crear objetivo
        Objective objective = bukkit.registerNewObjective(OBJECTIVE_NAME, "dummy", titleComponent);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        return new RenderedScoreboard(bukkit, objective, new HashMap<>());
    }

    /**
     * Actualiza el contenido de un scoreboard existente
     */
    public void updateScoreboard(RenderedScoreboard rendered, Player player,
                                 ScoreboardConfig config, ExyliaContext context) {

        // Actualizar título
        String processedTitle = context.processPlaceholders(config.getTitle(), player);
        Component titleComponent = ColorUtils.parse(processedTitle);
        rendered.objective.displayName(titleComponent);

        // Actualizar líneas
        List<String> lines = config.getLines();
        Map<Integer, String> lastContent = rendered.lastContent;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String processed = context.processPlaceholders(line, player);

            // Solo actualizar si cambió el contenido
            String previous = lastContent.get(i);
            if (!processed.equals(previous)) {
                updateLine(rendered, i, processed, lines.size());
                lastContent.put(i, processed);
            }
        }

        // Limpiar líneas no utilizadas
        cleanupUnusedLines(rendered, lines.size());
    }

    /**
     * Actualiza una línea específica del scoreboard
     */
    private void updateLine(RenderedScoreboard rendered, int position, String content, int totalLines) {
        String teamName = "line_" + position;
        Team team = rendered.scoreboard.getTeam(teamName);

        if (team == null) {
            team = rendered.scoreboard.registerNewTeam(teamName);
        }

        // Aplicar contenido con colores
        Component lineComponent = ColorUtils.parse(content);
        team.prefix(lineComponent);

        // Obtener entry único
        String entry = getUniqueEntry(position);
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }

        // Configurar score (líneas superiores tienen score mayor)
        Score score = rendered.objective.getScore(entry);
        score.setScore(totalLines - position);
    }

    /**
     * Limpia líneas no utilizadas
     * CORREGIDO: No usar removeIf en colección inmutable
     */
    private void cleanupUnusedLines(RenderedScoreboard rendered, int activeLines) {
        // Crear una lista de equipos a eliminar para evitar ConcurrentModificationException
        List<Team> teamsToRemove = new ArrayList<>();

        // Iterar sobre los equipos y encontrar los que deben eliminarse
        for (Team team : rendered.scoreboard.getTeams()) {
            String teamName = team.getName();
            if (teamName.startsWith("line_")) {
                try {
                    int position = Integer.parseInt(teamName.substring(5));
                    if (position >= activeLines) {
                        teamsToRemove.add(team);
                    }
                } catch (NumberFormatException ignored) {
                    // Si no se puede parsear el número, también eliminar por seguridad
                    teamsToRemove.add(team);
                }
            }
        }

        // Eliminar los equipos identificados
        for (Team team : teamsToRemove) {
            try {
                // Limpiar scores de todas las entradas del equipo
                for (String entry : team.getEntries()) {
                    rendered.scoreboard.resetScores(entry);
                }
                // Desregistrar el equipo
                team.unregister();
            } catch (Exception e) {
                // Log del error pero continuar con el siguiente
                System.err.println("Error eliminando equipo " + team.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene un entry único para una posición
     */
    private String getUniqueEntry(int position) {
        return entryCache.computeIfAbsent("pos_" + position, k -> {
            char[] colors = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            return "§" + colors[position % colors.length] + "§r";
        });
    }

    /**
     * Container para un scoreboard renderizado
     */
    public static class RenderedScoreboard {
        public final Scoreboard scoreboard;
        public final Objective objective;
        public final Map<Integer, String> lastContent;

        public RenderedScoreboard(Scoreboard scoreboard, Objective objective, Map<Integer, String> lastContent) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.lastContent = lastContent;
        }

        public void cleanup() {
            try {
                // Limpiar todos los scores primero
                if (objective != null) {
                    objective.unregister();
                }

                // Limpiar equipos de forma segura
                List<Team> teamsToCleanup = new ArrayList<>(scoreboard.getTeams());
                for (Team team : teamsToCleanup) {
                    try {
                        team.unregister();
                    } catch (Exception ignored) {
                        // Ignorar errores al limpiar equipos individuales
                    }
                }
            } catch (Exception ignored) {
                // Ignorar errores generales de limpieza
            }
        }
    }
}