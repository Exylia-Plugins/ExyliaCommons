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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ComponentScoreboardRenderer implements ScoreboardRenderer {

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

        PlaceholderContext finalContext = context != null ? context : PlaceholderContext.create();

        CompletableFuture<Component> titleFuture = processComponentAsync(
                scoreboard.getTitle(), player, finalContext
        );

        CompletableFuture<List<Component>> linesFuture = processLinesAsync(
                scoreboard.getLines(), player, finalContext
        );

        return CompletableFuture.allOf(titleFuture, linesFuture)
                .thenCompose(v -> {
                    Component processedTitle = titleFuture.join();
                    List<Component> processedLines = linesFuture.join();

                    CompletableFuture<Void> syncFuture = new CompletableFuture<>();
                    Tasks.sync(() -> {
                        if (!adapter.isDeleted()) {
                            adapter.updateTitle(processedTitle);
                            adapter.updateLines(processedLines);
                        }
                        syncFuture.complete(null);
                    });
                    return syncFuture;
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ScoreboardRenderException sre) throw sre;
                    throw new ScoreboardRenderException("Failed to render scoreboard", cause);
                });
    }

    @Override
    public void cleanup(Player player) {
    }

    private CompletableFuture<List<Component>> processLinesAsync(
            List<ScoreboardLine> lines, Player player, PlaceholderContext context
    ) {
        if (lines == null || lines.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<List<Component>>> lineFutures = lines.stream()
                .map(line -> Placeholders.processAsync(line.getContent(), player, context)
                        .thenApply(processed -> Arrays.stream(processed.split("\n", -1))
                                .map(ColorAPI::parse)
                                .collect(Collectors.toList())))
                .toList();

        return CompletableFuture.allOf(lineFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> lineFutures.stream()
                        .flatMap(f -> f.join().stream())
                        .collect(Collectors.toList()));
    }

    private CompletableFuture<Component> processComponentAsync(
            String text, Player player, PlaceholderContext context
    ) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(Component.empty());
        }

        return Placeholders.processAsync(text, player, context)
                .thenApply(ColorAPI::parse);
    }
}
