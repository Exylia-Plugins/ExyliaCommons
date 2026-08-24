package net.exylia.commons.v2.scoreboard.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.renderer.ComponentScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.renderer.ScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.renderer.SidebarHandle;
import org.bukkit.entity.Player;

import java.util.UUID;

@RequiredArgsConstructor
public class ScoreboardFactory {

    private final ScoreboardLibraryProvider provider;

    public ScoreboardInstance createInstance(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        ScoreboardRenderer renderer = new ComponentScoreboardRenderer(scoreboard);

        // Se reserva algo de holgura porque un placeholder con saltos de linea
        // puede expandirse a mas lineas de las declaradas en la plantilla.
        // SidebarHandle capa a Sidebar.MAX_LINES de todas formas.
        int capacity = Math.max(renderer.lineCount(), renderer.lineCount() + 2);

        return new ScoreboardInstance(
                UUID.randomUUID().toString(),
                player,
                scoreboard,
                new SidebarHandle(provider, player, capacity,
                        "exylia-" + UUID.randomUUID().toString().substring(0, 8)),
                renderer,
                context
        );
    }
}
