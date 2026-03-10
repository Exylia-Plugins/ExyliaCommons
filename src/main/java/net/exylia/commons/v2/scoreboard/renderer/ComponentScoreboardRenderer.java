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

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<Component>> processLinesAsync(
            List<ScoreboardLine> lines, Player player, PlaceholderContext context
    ) {
        if (lines == null || lines.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        int size = lines.size();
        CompletableFuture<List<Component>>[] lineFutures = new CompletableFuture[size];

        for (int i = 0; i < size; i++) {
            lineFutures[i] = Placeholders.processAsync(lines.get(i).getContent(), player, context)
                    .thenApply(processed -> {
                        if (!processed.contains("\n")) {
                            return List.of(ColorAPI.parse(processed));
                        }
                        String[] parts = processed.split("\n", -1);
                        List<Component> components = new ArrayList<>(parts.length);
                        for (String part : parts) {
                            components.add(ColorAPI.parse(part));
                        }
                        return components;
                    });
        }

        return CompletableFuture.allOf(lineFutures)
                .thenApply(v -> {
                    List<Component> result = new ArrayList<>();
                    for (CompletableFuture<List<Component>> future : lineFutures) {
                        result.addAll(future.join());
                    }
                    return result;
                });
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
