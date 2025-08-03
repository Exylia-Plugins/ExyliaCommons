package net.exylia.commons.scoreboard.internal;

import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.config.ScoreboardSettings;
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
 * Renderizador optimizado de scoreboards con configuración avanzada
 * Maneja la creación y actualización eficiente de scoreboards de Bukkit
 * Ahora incluye soporte para configuración de teams y comportamientos específicos
 */
public class ScoreboardRenderer {

    private static final ScoreboardManager BUKKIT_SCOREBOARD_MANAGER = Bukkit.getScoreboardManager();
    private static final String OBJECTIVE_NAME = "exylia_sb";

    private final ScoreboardSettings settings;

    // Cache para equipos reutilizables
    private final Map<String, String> entryCache = new HashMap<>();

    public ScoreboardRenderer(ScoreboardSettings settings) {
        this.settings = settings;
    }

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

        // Aplicar configuración de teams si está habilitada
        Team mainTeam = null;
        if (settings.hasCustomTeamSettings()) {
            mainTeam = setupMainTeam(bukkit, player);
        }

        return new RenderedScoreboard(bukkit, objective, new HashMap<>(), mainTeam);
    }

    /**
     * Configura el team principal con la configuración especificada
     */
    private Team setupMainTeam(Scoreboard scoreboard, Player player) {
        if (!settings.isCreateMainTeam()) {
            return null;
        }

        Team mainTeam = scoreboard.getTeam(settings.getMainTeamName());
        if (mainTeam == null) {
            mainTeam = scoreboard.registerNewTeam(settings.getMainTeamName());
        }

        // Aplicar configuraciones del team usando las APIs correctas
        mainTeam.setOption(Team.Option.COLLISION_RULE, settings.getCollisionRule());
        mainTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, settings.getNametagVisibility());
        mainTeam.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, settings.getDeathMessageVisibility());

        // Configuraciones que usan métodos específicos
        mainTeam.setCanSeeFriendlyInvisibles(settings.isCanSeeFriendlyInvisibles());
        mainTeam.setAllowFriendlyFire(settings.isAllowFriendlyFire());

        // Configurar colores y texto
        mainTeam.setColor(settings.getMainTeamColor());

        if (!settings.getMainTeamPrefix().isEmpty()) {
            Component prefixComponent = ColorUtils.parse(settings.getMainTeamPrefix());
            mainTeam.prefix(prefixComponent);
        }

        if (!settings.getMainTeamSuffix().isEmpty()) {
            Component suffixComponent = ColorUtils.parse(settings.getMainTeamSuffix());
            mainTeam.suffix(suffixComponent);
        }

        // Añadir el jugador al team
        mainTeam.addEntry(player.getName());

        return mainTeam;
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

        // Actualizar configuración del team principal si existe
        if (rendered.mainTeam != null) {
            updateMainTeam(rendered.mainTeam, player, context);
        }

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

        // Limpiar teams vacíos si está habilitado
        if (settings.isAutoCleanupEmptyTeams()) {
            cleanupEmptyTeams(rendered);
        }
    }

    /**
     * Actualiza el team principal con información dinámica
     */
    private void updateMainTeam(Team mainTeam, Player player, ExyliaContext context) {
        // Procesar prefijo y sufijo con placeholders si es necesario
        String prefix = settings.getMainTeamPrefix();
        String suffix = settings.getMainTeamSuffix();

        if (!prefix.isEmpty() && prefix.contains("%")) {
            String processedPrefix = context.processPlaceholders(prefix, player);
            Component prefixComponent = ColorUtils.parse(processedPrefix);
            mainTeam.prefix(prefixComponent);
        }

        if (!suffix.isEmpty() && suffix.contains("%")) {
            String processedSuffix = context.processPlaceholders(suffix, player);
            Component suffixComponent = ColorUtils.parse(processedSuffix);
            mainTeam.suffix(suffixComponent);
        }

        // Asegurar que el jugador esté en el team
        if (!mainTeam.hasEntry(player.getName())) {
            mainTeam.addEntry(player.getName());
        }
    }

    /**
     * Actualiza una línea específica del scoreboard
     */
    private void updateLine(RenderedScoreboard rendered, int position, String content, int totalLines) {
        String teamName = "line_" + position;
        Team team = rendered.scoreboard.getTeam(teamName);

        if (team == null) {
            team = rendered.scoreboard.registerNewTeam(teamName);

            // Aplicar configuraciones básicas a teams de líneas si es necesario
            if (settings.hasCustomTeamSettings()) {
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            }
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
     */
    private void cleanupUnusedLines(RenderedScoreboard rendered, int activeLines) {
        List<Team> teamsToRemove = new ArrayList<>();

        for (Team team : rendered.scoreboard.getTeams()) {
            String teamName = team.getName();
            if (teamName.startsWith("line_")) {
                try {
                    int position = Integer.parseInt(teamName.substring(5));
                    if (position >= activeLines) {
                        teamsToRemove.add(team);
                    }
                } catch (NumberFormatException ignored) {
                    teamsToRemove.add(team);
                }
            }
        }

        for (Team team : teamsToRemove) {
            try {
                for (String entry : team.getEntries()) {
                    rendered.scoreboard.resetScores(entry);
                }
                team.unregister();
            } catch (Exception e) {
                System.err.println("Error eliminando equipo " + team.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Limpia teams vacíos automáticamente
     */
    private void cleanupEmptyTeams(RenderedScoreboard rendered) {
        List<Team> teamsToRemove = new ArrayList<>();

        for (Team team : rendered.scoreboard.getTeams()) {
            // No limpiar el team principal ni los teams de líneas
            if (!team.getName().equals(settings.getMainTeamName()) &&
                    !team.getName().startsWith("line_") &&
                    team.getEntries().isEmpty()) {
                teamsToRemove.add(team);
            }
        }

        for (Team team : teamsToRemove) {
            try {
                team.unregister();
            } catch (Exception ignored) {
                // Ignorar errores de limpieza
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
     * Container para un scoreboard renderizado con team principal
     */
    public static class RenderedScoreboard {
        public final Scoreboard scoreboard;
        public final Objective objective;
        public final Map<Integer, String> lastContent;
        public final Team mainTeam; // Nuevo: team principal

        public RenderedScoreboard(Scoreboard scoreboard, Objective objective,
                                  Map<Integer, String> lastContent, Team mainTeam) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.lastContent = lastContent;
            this.mainTeam = mainTeam;
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