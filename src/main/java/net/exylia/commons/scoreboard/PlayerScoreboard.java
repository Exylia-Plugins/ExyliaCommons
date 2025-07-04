package net.exylia.commons.scoreboard;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Scoreboard individual con soporte de contexto personalizado
 */
class PlayerScoreboard {

    private final Plugin plugin;
    @Getter
    private final Player player;
    @Getter
    private final ScoreboardTemplate template;
    private final PlaceholderSystemManager placeholderManager;
    private final Scoreboard scoreboard;
    private final Objective objective;

    // Contextos específicos para este scoreboard
    private final ExyliaContext staticContext;

    // Cache para optimización
    private final Map<Integer, Team> teamCache = new HashMap<>();
    private final Map<Integer, String> lastContent = new HashMap<>();

    @Getter
    private boolean visible = false;
    private long lastUpdate = 0;

    private static final ScoreboardManager SCOREBOARD_MANAGER = Bukkit.getScoreboardManager();
    private static final String OBJECTIVE_NAME = "exylia";

    public PlayerScoreboard(Plugin plugin, Player player, ScoreboardTemplate template, ExyliaContext context) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.placeholderManager = PlaceholderSystemManager.getInstance();
        this.staticContext = context != null ? context : ExyliaContext.create();

        this.scoreboard = SCOREBOARD_MANAGER.getNewScoreboard();

        // Procesar título con sistema unificado
        String processedTitle = processText(template.getTitle());
        Component titleComponent = ColorUtils.parse(processedTitle);

        this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", titleComponent);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Procesa texto usando el sistema unificado de placeholders
     */
    private String processText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Combinar contexto estático con contexto dinámico del template
        ExyliaContext fullContext = staticContext.copy();

        // Añadir contexto dinámico del template
        ExyliaContext templateContext = template.getContext(player);
        if (templateContext != null) {
            fullContext.merge(templateContext);
        }

        return fullContext.processPlaceholders(text, player);
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
            long updateInterval = template.getUpdateTicks() * 50L; // Convertir ticks a ms
            if (currentTime - lastUpdate < updateInterval) {
                return this; // No es momento de actualizar aún
            }
        }

        lastUpdate = currentTime;

        try {
            // Actualizar título
            String processedTitle = processText(template.getTitle());
            Component titleComponent = ColorUtils.parse(processedTitle);
            objective.displayName(titleComponent);

            // Actualizar líneas
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
        // Procesar texto con sistema unificado
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

    private void cleanupUnusedLines(Set<Integer> activeLines) {
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

    /**
     * Añade contextos adicionales en tiempo de ejecución
     */
    public PlayerScoreboard addContexts(ExyliaContext additionalContext) {
        staticContext.merge(additionalContext);
        return this;
    }

    /**
     * Añade objetos al contexto
     */
    public PlayerScoreboard addToContext(Object... objects) {
        staticContext.addAll(objects);
        return this;
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

    public UUID getPlayerUUID() { return player.getUniqueId(); }
    private String getUniqueEntryName(int line) {
        char[] colors = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        return "§" + colors[line % colors.length] + "§r";
    }
}
