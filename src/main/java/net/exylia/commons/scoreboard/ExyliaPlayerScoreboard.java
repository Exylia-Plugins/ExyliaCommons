package net.exylia.commons.scoreboard;

import net.exylia.commons.config.components.ScoreboardConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.scoreboard.internal.PlayerScoreboardInstance;
import org.bukkit.entity.Player;

@Deprecated
public class ExyliaPlayerScoreboard {

    private final PlayerScoreboardInstance instance;

    ExyliaPlayerScoreboard(PlayerScoreboardInstance instance) {
        this.instance = instance;
    }

    public ExyliaPlayerScoreboard updateContext(ExyliaContext newContext) {
        instance.updateContext(newContext);
        return this;
    }

    public ExyliaPlayerScoreboard addToContext(Object... objects) {
        instance.addToContext(objects);
        return this;
    }

    public ExyliaPlayerScoreboard forceUpdate() {
        instance.update();
        return this;
    }

    public Player getPlayer() {
        return instance.getPlayer();
    }

    public boolean isVisible() {
        return instance.isVisible();
    }

    public ScoreboardConfig getConfig() {
        return instance.getConfig();
    }
}
