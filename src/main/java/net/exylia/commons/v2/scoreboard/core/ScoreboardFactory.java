package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import org.bukkit.entity.Player;

public final class ScoreboardFactory {
    public ScoreboardInstance create(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        return new ScoreboardInstance(player, scoreboard, context);
    }
}
