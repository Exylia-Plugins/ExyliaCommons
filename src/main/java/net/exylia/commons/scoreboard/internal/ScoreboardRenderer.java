package net.exylia.commons.scoreboard.internal;

import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.config.ScoreboardSettings;
import net.exylia.commons.scoreboard.fastBoard.FastBoard;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.DebugUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Renderizador optimizado de scoreboards usando FastBoard con Adventure Components
 * Mantiene la misma API pero usa FastBoard internamente para mejor rendimiento
 */
public class ScoreboardRenderer {

    private final ScoreboardSettings settings;

    public ScoreboardRenderer(ScoreboardSettings settings) {
        this.settings = settings;
    }

    /**
     * Crea un nuevo scoreboard para un jugador usando FastBoard
     */
    public RenderedScoreboard createScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
        // Crear FastBoard con Adventure Components
        FastBoard fastBoard = new FastBoard(player);

        // Procesar título inicial
        String processedTitle = context.processPlaceholders(config.getTitle(), player);
        Component titleComponent = ColorUtils.parse(processedTitle);
        fastBoard.updateTitle(LegacyComponentSerializer.legacySection().serialize(titleComponent));

        // Crear team principal si está configurado
        Team mainTeam = null;
        if (settings.hasCustomTeamSettings() && settings.isCreateMainTeam()) {
            mainTeam = setupMainTeam(fastBoard, player);
        }

        return new RenderedScoreboard(fastBoard, new HashMap<>(), mainTeam);
    }

    /**
     * Configura el team principal usando el scoreboard de FastBoard
     */
    private Team setupMainTeam(FastBoard fastBoard, Player player) {
        try {
            // Acceder al scoreboard interno de FastBoard
            org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

            Team mainTeam = scoreboard.getTeam(settings.getMainTeamName());
            if (mainTeam == null) {
                mainTeam = scoreboard.registerNewTeam(settings.getMainTeamName());
            }

            // Aplicar configuraciones del team
            mainTeam.setOption(Team.Option.COLLISION_RULE, settings.getCollisionRule());
            mainTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, settings.getNametagVisibility());
            mainTeam.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, settings.getDeathMessageVisibility());

            mainTeam.setCanSeeFriendlyInvisibles(settings.isCanSeeFriendlyInvisibles());
            mainTeam.setAllowFriendlyFire(settings.isAllowFriendlyFire());
            mainTeam.setColor(settings.getMainTeamColor());

            // Usar Components para prefijo y sufijo
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
        } catch (Exception e) {
            DebugUtils.logError("Error configurando team principal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza el contenido de un scoreboard usando FastBoard
     */
    public void updateScoreboard(RenderedScoreboard rendered, Player player,
                                 ScoreboardConfig config, ExyliaContext context) {

        FastBoard fastBoard = rendered.fastBoard;

        // Actualizar título si cambió
        String processedTitle = context.processPlaceholders(config.getTitle(), player);
        if (!processedTitle.equals(rendered.lastContent.get("title"))) {
            Component titleComponent = ColorUtils.parse(processedTitle);
            // FastBoard Adventure requiere String para el título
            String titleString = LegacyComponentSerializer.legacySection().serialize(titleComponent);
            fastBoard.updateTitle(titleString);
            rendered.lastContent.put("title", processedTitle);
        }

        // Actualizar configuración del team principal si existe
        if (rendered.mainTeam != null) {
            updateMainTeam(rendered.mainTeam, player, context);
        }

        // Procesar líneas
        List<String> lines = config.getLines();
        String[] processedLines = new String[lines.size()];
        boolean hasChanges = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String processed = context.processPlaceholders(line, player);

            // Solo actualizar si cambió el contenido
            String previous = rendered.lastContent.get("line_" + i);
            if (!processed.equals(previous)) {
                hasChanges = true;
                rendered.lastContent.put("line_" + i, processed);
            }

            // Convertir Component a String legacy para FastBoard
            Component lineComponent = ColorUtils.parse(processed);
            processedLines[i] = LegacyComponentSerializer.legacySection().serialize(lineComponent);
        }

        // Solo actualizar líneas si hubo cambios
        if (hasChanges) {
            fastBoard.updateLines(processedLines);
        }

        // Limpiar teams vacíos si está habilitado
        if (settings.isAutoCleanupEmptyTeams()) {
            cleanupEmptyTeams(rendered, player);
        }
    }

    /**
     * Actualiza el team principal con información dinámica
     */
    private void updateMainTeam(Team mainTeam, Player player, ExyliaContext context) {
        try {
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
        } catch (Exception e) {
            DebugUtils.logError("Error actualizando team principal: " + e.getMessage());
        }
    }

    /**
     * Limpia teams vacíos automáticamente
     */
    private void cleanupEmptyTeams(RenderedScoreboard rendered, Player player) {
        if (!settings.isAutoCleanupEmptyTeams()) return;

        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

            scoreboard.getTeams().stream()
                    .filter(team -> !team.getName().equals(settings.getMainTeamName()))
                    .filter(team -> team.getEntries().isEmpty())
                    .forEach(team -> {
                        try {
                            team.unregister();
                        } catch (Exception ignored) {
                            // Ignorar errores de limpieza
                        }
                    });
        } catch (Exception e) {
            DebugUtils.logError("Error limpiando teams vacíos: " + e.getMessage());
        }
    }

    /**
     * Container para un scoreboard renderizado usando FastBoard con Adventure
     */
    public static class RenderedScoreboard {
        public final FastBoard fastBoard;
        public final Map<String, String> lastContent;
        public final Team mainTeam;

        public RenderedScoreboard(FastBoard fastBoard, Map<String, String> lastContent, Team mainTeam) {
            this.fastBoard = fastBoard;
            this.lastContent = lastContent;
            this.mainTeam = mainTeam;
        }

        public void cleanup() {
            try {
                if (fastBoard != null && !fastBoard.isDeleted()) {
                    fastBoard.delete();
                }

                // Limpiar team principal si existe
                if (mainTeam != null) {
                    try {
                        mainTeam.unregister();
                    } catch (Exception ignored) {
                        // El team puede ya estar eliminado
                    }
                }
            } catch (Exception e) {
                DebugUtils.logError("Error limpiando scoreboard: " + e.getMessage());
            }
        }

        // Métodos de compatibilidad para acceso directo al scoreboard
        public org.bukkit.scoreboard.Scoreboard getScoreboard() {
            return fastBoard.getPlayer().getScoreboard();
        }
    }
}