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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderizador optimizado de scoreboards usando FastBoard con Adventure Components
 * Mantiene la misma API pero usa FastBoard internamente para mejor rendimiento
 * Optimizado con caché local para componentes estáticos
 */
public class ScoreboardRenderer {

    private final ScoreboardSettings settings;
    
    // Cache local para componentes estáticos (títulos, líneas sin placeholders)
    private static final Map<String, String> STATIC_COMPONENT_CACHE = new ConcurrentHashMap<>();
    
    // Cache para componentes procesados por jugador (para reducir re-parsing)
    private final Map<String, Map<String, String>> playerComponentCache = new ConcurrentHashMap<>();

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
//        if (settings.hasCustomTeamSettings() && settings.isCreateMainTeam()) {
//            mainTeam = setupMainTeam(fastBoard, player);
//        }

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
            DebugUtils.logInternalError("Error configurando team principal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza el contenido de un scoreboard usando FastBoard - OPTIMIZADO
     */
    public void updateScoreboard(RenderedScoreboard rendered, Player player,
                                 ScoreboardConfig config, ExyliaContext context) {

        FastBoard fastBoard = rendered.fastBoard;
        String playerId = player.getUniqueId().toString();
        Map<String, String> playerCache = playerComponentCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        // Actualizar título de forma optimizada
        String titleString = processAndCacheComponent(config.getTitle(), "title", playerId, player, context, rendered);
        if (titleString != null) {
            fastBoard.updateTitle(titleString);
        }

        // Actualizar configuración del team principal si existe
        if (rendered.mainTeam != null) {
//            updateMainTeam(rendered.mainTeam, player, context);
        }

        // Procesar líneas de forma optimizada con batch processing
        List<String> lines = config.getLines();
        String[] processedLines = new String[lines.size()];
        boolean hasChanges = false;

        // Pre-procesar todas las líneas en batch
        for (int i = 0; i < lines.size(); i++) {
            String lineKey = "line_" + i;
            String processedLineString = processAndCacheComponent(lines.get(i), lineKey, playerId, player, context, rendered);
            
            if (processedLineString != null) {
                hasChanges = true;
                processedLines[i] = processedLineString;
            } else {
                // Reutilizar la línea previamente procesada
                String previous = rendered.lastContent.get(lineKey);
                if (previous != null) {
                    String cachedLine = playerCache.get(lineKey + "_serialized");
                    processedLines[i] = cachedLine != null ? cachedLine : 
                        LegacyComponentSerializer.legacySection().serialize(ColorUtils.parse(previous));
                }
            }
        }

        // Solo actualizar líneas si hubo cambios
        if (hasChanges) {
            fastBoard.updateLines(processedLines);
        }

        // Limpiar teams vacíos si está habilitado (solo cada 10 actualizaciones para reducir overhead)
        if (settings.isAutoCleanupEmptyTeams() && System.currentTimeMillis() % 10 == 0) {
            cleanupEmptyTeams(rendered, player);
        }
    }

    /**
     * Procesa y cachea un componente, solo actualizando si cambió
     * @return String serializado si cambió, null si no cambió
     */
    private String processAndCacheComponent(String template, String key, String playerId, 
                                          Player player, ExyliaContext context, RenderedScoreboard rendered) {
        
        // Verificar si es contenido estático (sin placeholders)
        boolean isStatic = !template.contains("%");
        
        if (isStatic) {
            // Para contenido estático, usar cache global
            return STATIC_COMPONENT_CACHE.computeIfAbsent(template, t -> {
                Component component = ColorUtils.parse(t);
                return LegacyComponentSerializer.legacySection().serialize(component);
            });
        }
        
        // Para contenido dinámico
        String processed = context.processPlaceholders(template, player);
        String previous = rendered.lastContent.get(key);
        
        if (!processed.equals(previous)) {
            rendered.lastContent.put(key, processed);
            
            // Cachear la versión serializada para evitar re-serialización
            Component component = ColorUtils.parse(processed);
            String serialized = LegacyComponentSerializer.legacySection().serialize(component);
            
            Map<String, String> playerCache = playerComponentCache.get(playerId);
            if (playerCache != null) {
                playerCache.put(key + "_serialized", serialized);
            }
            
            return serialized;
        }
        
        return null; // No cambió
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
            DebugUtils.logInternalError("Error actualizando team principal: " + e.getMessage());
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
            DebugUtils.logInternalError("Error limpiando teams vacíos: " + e.getMessage());
        }
    }
    
    /**
     * Limpia el cache para un jugador específico cuando se desconecta
     * Debe ser llamado desde el ScoreboardManager cuando un jugador se desconecta
     */
    public void clearPlayerCache(String playerId) {
        playerComponentCache.remove(playerId);
    }
    
    /**
     * Limpia todos los caches para liberar memoria
     */
    public static void clearAllCaches() {
        STATIC_COMPONENT_CACHE.clear();
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
                DebugUtils.logInternalError("Error limpiando scoreboard: " + e.getMessage());
            }
        }

        // Métodos de compatibilidad para acceso directo al scoreboard
        public org.bukkit.scoreboard.Scoreboard getScoreboard() {
            return fastBoard.getPlayer().getScoreboard();
        }
    }
}