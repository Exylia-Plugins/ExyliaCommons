package net.exylia.commons.v2.scoreboard.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.renderer.ComponentScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.renderer.FastBoardComponentAdapter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@RequiredArgsConstructor
public class ScoreboardFactory {

    private final Plugin plugin;

    public ScoreboardInstance createInstance(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        return new ScoreboardInstance(
                UUID.randomUUID().toString(),
                player,
                scoreboard,
                new FastBoardComponentAdapter(player),
                new ComponentScoreboardRenderer(scoreboard),
                context
        );
    }
}
