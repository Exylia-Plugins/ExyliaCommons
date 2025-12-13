package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.cache.LineCacheKey;
import net.exylia.commons.v2.scoreboard.cache.ScoreboardCacheManager;
import net.exylia.commons.v2.scoreboard.exception.ScoreboardRenderException;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class ComponentScoreboardRenderer {

    private final ScoreboardCacheManager cacheManager;

    public CompletableFuture<Void> renderAsync(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context,
            FastBoardComponentAdapter adapter
    ) {
        if (adapter.isDeleted()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            PlaceholderContext finalContext = context != null ? context : PlaceholderContext.create();

            CompletableFuture<Component> titleFuture = processStringAsync(
                    scoreboard.getTitle(),
                    player,
                    finalContext
            );

            CompletableFuture<List<Component>> linesFuture = processLinesAsync(
                    scoreboard.getLines(),
                    player,
                    finalContext
            );

            return CompletableFuture.allOf(titleFuture, linesFuture)
                    .thenApply(v -> {
                        Component processedTitle = titleFuture.join();
                        List<Component> processedLines = linesFuture.join();

                        Schedulers.sync(() -> {
                            if (!adapter.isDeleted()) {
                                adapter.updateTitle(processedTitle);
                                adapter.updateLines(processedLines);
                            }
                        });

                        return (Void) null;
                    })
                    .exceptionally(ex -> {
                        if (ex instanceof ScoreboardRenderException) {
                            throw (ScoreboardRenderException) ex;
                        }
                        throw new ScoreboardRenderException("Failed to render scoreboard", ex);
                    });

        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                    new ScoreboardRenderException("Failed to render scoreboard", e)
            );
        }
    }

    public void cleanup(Player player) {
        cacheManager.invalidatePlayer(player.getUniqueId());
    }

    private CompletableFuture<List<Component>> processLinesAsync(
            List<ScoreboardLine> lines,
            Player player,
            PlaceholderContext context
    ) {
        if (lines == null || lines.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<Component>> lineFutures = lines.stream()
                .map(line -> processLineAsync(line, player, context))
                .toList();

        return CompletableFuture.allOf(lineFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> lineFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }

    private CompletableFuture<Component> processLineAsync(
            ScoreboardLine line,
            Player player,
            PlaceholderContext context
    ) {
        return processStringAsync(line.getContent(), player, context);
    }

    private CompletableFuture<Component> processStringAsync(
            String text,
            Player player,
            PlaceholderContext context
    ) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(Component.empty());
        }

        return Placeholders.processAsync(text, player, context)
                .thenApply(ColorAPI::parse);
    }
}
