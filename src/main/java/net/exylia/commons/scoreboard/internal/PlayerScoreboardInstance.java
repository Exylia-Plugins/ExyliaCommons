package net.exylia.commons.scoreboard.internal;

import lombok.Getter;
import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.internal.ScoreboardRenderer.RenderedScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.util.Set;
import java.util.HashSet;

/**
 * Instancia de scoreboard para un jugador específico
 * Maneja el estado interno y la lógica de actualización
 * Ahora incluye gestión avanzada de teams
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
        this.context = context.copy(); // Copia defensiva
        this.renderer = renderer;
    }

    /**
     * Muestra el scoreboard al jugador
     */
    public void show() {
        if (visible || !player.isOnline()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (visible || !player.isOnline()) return;

            rendered = renderer.createScoreboard(player, config, context);
            player.setScoreboard(rendered.scoreboard);
            visible = true;

            // Añadir el jugador al team principal automáticamente
            if (rendered.mainTeam != null) {
                teamMembers.add(player.getName());
            }

            update();
        });
    }

    /**
     * Oculta el scoreboard del jugador
     */
    public void hide() {
        if (!visible) return;

        visible = false;

        if (player.isOnline()) {
            // Restaurar scoreboard principal
            player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
        }

        // Limpiar recursos
        if (rendered != null) {
            rendered.cleanup();
            rendered = null;
        }

        // Limpiar miembros del team
        teamMembers.clear();
    }

    /**
     * Actualiza el contenido del scoreboard
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
            plugin.getLogger().warning("Error actualizando scoreboard de " + player.getName() + ": " + e.getMessage());
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
            plugin.getLogger().warning("Error añadiendo " + targetPlayer.getName() +
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
            plugin.getLogger().warning("Error removiendo " + targetPlayer.getName() +
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
            plugin.getLogger().warning("Error limpiando team principal de " + player.getName() + ": " + e.getMessage());
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
            Team existingTeam = rendered.scoreboard.getTeam(teamName);
            if (existingTeam != null) {
                return existingTeam;
            }

            return rendered.scoreboard.registerNewTeam(teamName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error creando team personalizado '" + teamName +
                    "' para " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene un team por nombre
     */
    public Team getTeam(String teamName) {
        return (visible && rendered != null) ? rendered.scoreboard.getTeam(teamName) : null;
    }

    /**
     * Elimina un team personalizado
     */
    public boolean removeCustomTeam(String teamName) {
        if (!visible || rendered == null) {
            return false;
        }

        try {
            Team team = rendered.scoreboard.getTeam(teamName);
            if (team != null && !team.equals(rendered.mainTeam) && !teamName.startsWith("line_")) {
                team.unregister();
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error eliminando team personalizado '" + teamName +
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
}