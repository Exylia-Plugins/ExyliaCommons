package net.exylia.commons.scoreboard;

import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.internal.PlayerScoreboardInstance;
import org.bukkit.entity.Player;

public class ExyliaPlayerScoreboard {

    private final PlayerScoreboardInstance instance;

    ExyliaPlayerScoreboard(PlayerScoreboardInstance instance) {
        this.instance = instance;
    }

    /**
     * Actualiza el contexto del scoreboard
     */
    public ExyliaPlayerScoreboard updateContext(ExyliaContext newContext) {
        instance.updateContext(newContext);
        return this;
    }

    /**
     * Añade objetos al contexto
     */
    public ExyliaPlayerScoreboard addToContext(Object... objects) {
        instance.addToContext(objects);
        return this;
    }

    /**
     * Fuerza una actualización inmediata
     */
    public ExyliaPlayerScoreboard forceUpdate() {
        instance.update();
        return this;
    }

    /**
     * Obtiene el jugador propietario
     */
    public Player getPlayer() {
        return instance.getPlayer();
    }

    /**
     * Verifica si está visible
     */
    public boolean isVisible() {
        return instance.isVisible();
    }

    /**
     * Obtiene la configuración utilizada
     */
    public ScoreboardConfig getConfig() {
        return instance.getConfig();
    }
}