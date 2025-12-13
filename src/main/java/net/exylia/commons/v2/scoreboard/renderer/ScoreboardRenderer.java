package net.exylia.commons.v2.scoreboard.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface ScoreboardRenderer {

    CompletableFuture<Void> renderAsync(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context,
            FastBoardAdapter adapter
    );

    void cleanup(Player player);
}
