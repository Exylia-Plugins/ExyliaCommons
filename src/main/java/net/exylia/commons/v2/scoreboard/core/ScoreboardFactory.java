package net.exylia.commons.v2.scoreboard.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.cache.ScoreboardCacheManager;
import net.exylia.commons.v2.scoreboard.config.TeamConfig;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.renderer.ComponentScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.renderer.FastBoardComponentAdapter;
import net.exylia.commons.v2.scoreboard.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@RequiredArgsConstructor
public class ScoreboardFactory {

    private final Plugin plugin;
    private final ScoreboardCacheManager cacheManager;

    public ScoreboardInstance createInstance(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context
    ) {
        String instanceId = UUID.randomUUID().toString();

        FastBoardComponentAdapter fastBoardAdapter = createFastBoardAdapter(player);

        TeamManager teamManager = scoreboard.hasTeam()
                ? createTeamManager(player, scoreboard.getTeamConfig(), fastBoardAdapter)
                : null;

        ComponentScoreboardRenderer renderer = new ComponentScoreboardRenderer(cacheManager);

        return new ScoreboardInstance(
                instanceId,
                player,
                scoreboard,
                fastBoardAdapter,
                teamManager,
                renderer,
                context
        );
    }

    public FastBoardComponentAdapter createFastBoardAdapter(Player player) {
        return new FastBoardComponentAdapter(player);
    }

    public TeamManager createTeamManager(Player player, TeamConfig teamConfig, FastBoardComponentAdapter adapter) {
        if (teamConfig == null) {
            return null;
        }

        return new TeamManager(player, teamConfig, adapter.getBukkitScoreboard());
    }
}
