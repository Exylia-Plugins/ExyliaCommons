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

public class ScoreboardRenderer {

    private final ScoreboardSettings settings;
    
    private static final Map<String, String> STATIC_COMPONENT_CACHE = new ConcurrentHashMap<>();
    
    private final Map<String, Map<String, String>> playerComponentCache = new ConcurrentHashMap<>();

    public ScoreboardRenderer(ScoreboardSettings settings) {
        this.settings = settings;
    }

    public RenderedScoreboard createScoreboard(Player player, ScoreboardConfig config, ExyliaContext context) {
         
        FastBoard fastBoard = new FastBoard(player);

        String processedTitle = context.processPlaceholders(config.getTitle(), player);
        Component titleComponent = ColorUtils.parse(processedTitle);
        fastBoard.updateTitle(LegacyComponentSerializer.legacySection().serialize(titleComponent));

        Team mainTeam = null;
 
        return new RenderedScoreboard(fastBoard, new HashMap<>(), mainTeam);
    }

    private Team setupMainTeam(FastBoard fastBoard, Player player) {
        try {
             
            org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();

            Team mainTeam = scoreboard.getTeam(settings.getMainTeamName());
            if (mainTeam == null) {
                mainTeam = scoreboard.registerNewTeam(settings.getMainTeamName());
            }

            mainTeam.setOption(Team.Option.COLLISION_RULE, settings.getCollisionRule());
            mainTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, settings.getNametagVisibility());
            mainTeam.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, settings.getDeathMessageVisibility());

            mainTeam.setCanSeeFriendlyInvisibles(settings.isCanSeeFriendlyInvisibles());
            mainTeam.setAllowFriendlyFire(settings.isAllowFriendlyFire());
            mainTeam.setColor(settings.getMainTeamColor());

            if (!settings.getMainTeamPrefix().isEmpty()) {
                Component prefixComponent = ColorUtils.parse(settings.getMainTeamPrefix());
                mainTeam.prefix(prefixComponent);
            }

            if (!settings.getMainTeamSuffix().isEmpty()) {
                Component suffixComponent = ColorUtils.parse(settings.getMainTeamSuffix());
                mainTeam.suffix(suffixComponent);
            }

            mainTeam.addEntry(player.getName());

            return mainTeam;
        } catch (Exception e) {
            DebugUtils.logInternalError("Error configurando team principal: " + e.getMessage());
            return null;
        }
    }

    public void updateScoreboard(RenderedScoreboard rendered, Player player,
                                 ScoreboardConfig config, ExyliaContext context) {

        FastBoard fastBoard = rendered.fastBoard;
        String playerId = player.getUniqueId().toString();
        Map<String, String> playerCache = playerComponentCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        String titleString = processAndCacheComponent(config.getTitle(), "title", playerId, player, context, rendered);
        if (titleString != null) {
            fastBoard.updateTitle(titleString);
        }

        if (rendered.mainTeam != null) {
 
        }

        List<String> lines = config.getLines();
        String[] processedLines = new String[lines.size()];
        boolean hasChanges = false;

        for (int i = 0; i < lines.size(); i++) {
            String lineKey = "line_" + i;
            String processedLineString = processAndCacheComponent(lines.get(i), lineKey, playerId, player, context, rendered);
            
            if (processedLineString != null) {
                hasChanges = true;
                processedLines[i] = processedLineString;
            } else {
                 
                String previous = rendered.lastContent.get(lineKey);
                if (previous != null) {
                    String cachedLine = playerCache.get(lineKey + "_serialized");
                    processedLines[i] = cachedLine != null ? cachedLine : 
                        LegacyComponentSerializer.legacySection().serialize(ColorUtils.parse(previous));
                }
            }
        }

        if (hasChanges) {
            fastBoard.updateLines(processedLines);
        }

        if (settings.isAutoCleanupEmptyTeams() && System.currentTimeMillis() % 10 == 0) {
            cleanupEmptyTeams(rendered, player);
        }
    }

    private String processAndCacheComponent(String template, String key, String playerId, 
                                          Player player, ExyliaContext context, RenderedScoreboard rendered) {
        
        boolean isStatic = !template.contains("%");
        
        if (isStatic) {
             
            return STATIC_COMPONENT_CACHE.computeIfAbsent(template, t -> {
                Component component = ColorUtils.parse(t);
                return LegacyComponentSerializer.legacySection().serialize(component);
            });
        }
        
        String processed = context.processPlaceholders(template, player);
        String previous = rendered.lastContent.get(key);
        
        if (!processed.equals(previous)) {
            rendered.lastContent.put(key, processed);
            
            Component component = ColorUtils.parse(processed);
            String serialized = LegacyComponentSerializer.legacySection().serialize(component);
            
            Map<String, String> playerCache = playerComponentCache.get(playerId);
            if (playerCache != null) {
                playerCache.put(key + "_serialized", serialized);
            }
            
            return serialized;
        }
        
        return null;  
    }

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

            if (!mainTeam.hasEntry(player.getName())) {
                mainTeam.addEntry(player.getName());
            }
        } catch (Exception e) {
            DebugUtils.logInternalError("Error actualizando team principal: " + e.getMessage());
        }
    }

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
                             
                        }
                    });
        } catch (Exception e) {
            DebugUtils.logInternalError("Error limpiando teams vacíos: " + e.getMessage());
        }
    }
    
    public void clearPlayerCache(String playerId) {
        playerComponentCache.remove(playerId);
    }
    
    public static void clearAllCaches() {
        STATIC_COMPONENT_CACHE.clear();
    }
    
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

                if (mainTeam != null) {
                    try {
                        mainTeam.unregister();
                    } catch (Exception ignored) {
                         
                    }
                }
            } catch (Exception e) {
                DebugUtils.logInternalError("Error limpiando scoreboard: " + e.getMessage());
            }
        }

        public org.bukkit.scoreboard.Scoreboard getScoreboard() {
            return fastBoard.getPlayer().getScoreboard();
        }
    }
}
