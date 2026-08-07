package net.exylia.commons.v2.scoreboard.renderer;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.exception.ScoreboardRenderException;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ComponentScoreboardRenderer implements ScoreboardRenderer {

    private final int lineCount;
    private final String[] lineContents;
    private final boolean[] lineDynamic;
    private final Component[] staticLineCache;
    private final String titleContent;
    private final boolean titleDynamic;
    private final Component staticTitle;

    public ComponentScoreboardRenderer(Scoreboard scoreboard) {
        List<ScoreboardLine> lines = scoreboard.getLines();
        this.lineCount = lines != null ? lines.size() : 0;
        this.lineContents = new String[lineCount];
        this.lineDynamic = new boolean[lineCount];
        this.staticLineCache = new Component[lineCount];

        for (int i = 0; i < lineCount; i++) {
            ScoreboardLine line = lines.get(i);
            String content = line.getContent() != null ? line.getContent() : "";
            lineContents[i] = content;
            if (line.isDynamic()) {
                lineDynamic[i] = true;
            } else {
                staticLineCache[i] = ColorAPI.parse(content);
            }
        }

        String title = scoreboard.getTitle();
        this.titleContent = title != null ? title : "";
        this.titleDynamic = titleContent.contains("%");
        this.staticTitle = titleDynamic ? null : ColorAPI.parse(titleContent);
    }

    @Override
    public CompletableFuture<Void> renderAsync(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context,
            FastBoardComponentAdapter adapter
    ) {
        if (adapter.isDeleted()) {
            return CompletableFuture.completedFuture(null);
        }

        PlaceholderContext ctx = context != null ? context : PlaceholderContext.create();

        return Tasks.computeValue(() -> buildRenderResult(player, ctx))
                .thenCompose(result -> {
                    CompletableFuture<Void> sync = new CompletableFuture<>();
                    Tasks.sync(() -> {
                        if (!adapter.isDeleted()) {
                            adapter.updateTitle(result.title());
                            adapter.updateLines(result.lines());
                        }
                        sync.complete(null);
                    });
                    return sync;
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ScoreboardRenderException sre) throw sre;
                    throw new ScoreboardRenderException("Failed to render scoreboard", cause);
                });
    }

    private RenderResult buildRenderResult(Player player, PlaceholderContext ctx) {
        Component title = titleDynamic
                ? ColorAPI.parse(Placeholders.process(titleContent, player, ctx))
                : staticTitle;

        List<Component> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            if (lineDynamic[i]) {
                String processed = Placeholders.process(lineContents[i], player, ctx);
                if (processed.contains("\n")) {
                    for (String part : processed.split("\n", -1)) {
                        lines.add(ColorAPI.parse(part));
                    }
                } else {
                    lines.add(ColorAPI.parse(processed));
                }
            } else {
                lines.add(staticLineCache[i]);
            }
        }

        return new RenderResult(title, lines);
    }

    private record RenderResult(Component title, List<Component> lines) {}

    @Override
    public void cleanup(Player player) {
    }
}
