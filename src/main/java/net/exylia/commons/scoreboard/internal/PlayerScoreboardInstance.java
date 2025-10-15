package net.exylia.commons.scoreboard.internal;

import lombok.Getter;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.fastBoard.FastBoard;
import net.exylia.commons.scoreboard.internal.ScoreboardRenderer.RenderedScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.util.Set;
import java.util.HashSet;

import static net.exylia.commons.utils.DebugUtils.logInternalDebug;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

/**
 * Instancia de scoreboard para un jugador específico usando FastBoard con Adventure Components
 * Mantiene la misma API pero usa FastBoard internamente
 */
public class PlayerScoreboardInstance {

    private final Plugin plugin;
    @Getter
    private final Player player;
    @Getter
    private final ScoreboardConfig config;
    private final ScoreboardRenderer renderer;

    private ExyliaContext context;
    private RenderedScoreboard rendered;

    @Getter
    private boolean visible = false;
    private long lastUpdate = 0;

    // Gestión de jugadores en el team principal
    private final Set<String> teamMembers = new HashSet<>();

    public PlayerScoreboardInstance(Plugin plugin, Player player, ScoreboardConfig config,
                                    ExyliaContext context, ScoreboardRenderer renderer) {
        this.plugin = plugin;
        this.player = player;
        this.config = config;
        this.context = context.copy();
        this.renderer = renderer;
    }

    /**
     * Muestra el scoreboard al jugador usando FastBoard
     */
    public void show() {
        if (visible || !player.isOnline()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (visible || !player.isOnline()) return;

            try {
                rendered = renderer.createScoreboard(player, config, context);
                visible = true;

                // Añadir el jugador al team principal automáticamente
                if (rendered.mainTeam != null) {
                    teamMembers.add(player.getName());
                }

                update();
            } catch (Exception e) {
                logInternalWarn("Error mostrando scoreboard para " + player.getName() + ": " + e.getMessage());
                visible = false;
            }
        });
    }

    /**
     * Oculta el scoreboard del jugador
     */
    public void hide() {
        if (!visible) return;

        visible = false;

        try {
            // Limpiar recursos
            if (rendered != null) {
                rendered.cleanup();
                rendered = null;
            }
        } catch (Exception e) {
            logInternalWarn("Error ocultando scoreboard de " + player.getName() + ": " + e.getMessage());
        }

        // Limpiar miembros del team
        teamMembers.clear();
    }

    /**
     * Actualiza el contenido del scoreboard usando FastBoard
     */
    public void update() {
        if (!visible || !player.isOnline() || rendered == null) {
            hide();
            return;
        }

        try {
            renderer.updateScoreboard(rendered, player, config, context);
            lastUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            logInternalDebug("Error actualizando scoreboard de " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si el scoreboard debe actualizarse según su configuración
     */
    public boolean shouldUpdate() {
        if (!visible || config.getUpdateInterval() <= 0) {
            return false;
        }

        long interval = config.getUpdateInterval() * 50L; // Convertir ticks a ms
        return (System.currentTimeMillis() - lastUpdate) >= interval;
    }

    /**
     * Actualiza el contexto completo
     */
    public void updateContext(ExyliaContext newContext) {
        this.context = newContext.copy();
    }

    /**
     * Añade objetos al contexto existente
     */
    public void addToContext(Object... objects) {
        this.context.addAll(objects);
    }

    // ==================== GESTIÓN DE TEAMS ====================

    /**
     * Añade un jugador al team principal del scoreboard
     */
    public boolean addPlayerToMainTeam(Player targetPlayer) {
        if (!visible || rendered == null || rendered.mainTeam == null) {
            return false;
        }

        try {
            String playerName = targetPlayer.getName();
            if (!teamMembers.contains(playerName)) {
                rendered.mainTeam.addEntry(playerName);
                teamMembers.add(playerName);
                return true;
            }
        } catch (Exception e) {
            logInternalWarn("Error añadiendo " + targetPlayer.getName() +
                    " al team principal de " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Remueve un jugador del team principal
     */
    public boolean removePlayerFromMainTeam(Player targetPlayer) {
        if (!visible || rendered == null || rendered.mainTeam == null) {
            return false;
        }

        try {
            String playerName = targetPlayer.getName();
            if (teamMembers.contains(playerName)) {
                rendered.mainTeam.removeEntry(playerName);
                teamMembers.remove(playerName);
                return true;
            }
        } catch (Exception e) {
            logInternalWarn("Error removiendo " + targetPlayer.getName() +
                    " del team principal de " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica si un jugador está en el team principal
     */
    public boolean isPlayerInMainTeam(Player targetPlayer) {
        return teamMembers.contains(targetPlayer.getName());
    }

    /**
     * Obtiene todos los miembros del team principal
     */
    public Set<String> getMainTeamMembers() {
        return new HashSet<>(teamMembers);
    }

    /**
     * Limpia todos los miembros del team principal excepto el owner
     */
    public void clearMainTeam() {
        if (!visible || rendered == null || rendered.mainTeam == null) {
            return;
        }

        try {
            // Remover todos excepto el propietario
            Set<String> toRemove = new HashSet<>(teamMembers);
            toRemove.remove(player.getName());

            for (String memberName : toRemove) {
                rendered.mainTeam.removeEntry(memberName);
            }

            teamMembers.clear();
            teamMembers.add(player.getName()); // Mantener solo al propietario
        } catch (Exception e) {
            logInternalWarn("Error limpiando team principal de " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene el team principal si existe
     */
    public Team getMainTeam() {
        return (visible && rendered != null) ? rendered.mainTeam : null;
    }

    /**
     * Crea un team personalizado en este scoreboard
     */
    public Team createCustomTeam(String teamName) {
        if (!visible || rendered == null) {
            return null;
        }

        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = rendered.getScoreboard();
            Team existingTeam = scoreboard.getTeam(teamName);
            if (existingTeam != null) {
                return existingTeam;
            }

            return scoreboard.registerNewTeam(teamName);
        } catch (Exception e) {
            logInternalWarn("Error creando team personalizado '" + teamName +
                    "' para " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene un team por nombre
     */
    public Team getTeam(String teamName) {
        if (!visible || rendered == null) {
            return null;
        }

        try {
            return rendered.getScoreboard().getTeam(teamName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Elimina un team personalizado
     */
    public boolean removeCustomTeam(String teamName) {
        if (!visible || rendered == null) {
            return false;
        }

        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = rendered.getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team != null && !team.equals(rendered.mainTeam)) {
                team.unregister();
                return true;
            }
        } catch (Exception e) {
            logInternalWarn("Error eliminando team personalizado '" + teamName +
                    "' de " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    // ==================== GETTERS ====================

    public ExyliaContext getContext() {
        return context.copy();
    }

    public boolean hasMainTeam() {
        return visible && rendered != null && rendered.mainTeam != null;
    }

    public int getMainTeamSize() {
        return teamMembers.size();
    }

    public FastBoard getFastBoard() {
        return (visible && rendered != null) ? rendered.fastBoard : null;
    }
}