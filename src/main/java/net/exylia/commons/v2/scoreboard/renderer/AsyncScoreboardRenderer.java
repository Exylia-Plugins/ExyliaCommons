package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.cache.LineCacheKey;
import net.exylia.commons.v2.scoreboard.cache.ScoreboardCacheManager;
import net.exylia.commons.v2.scoreboard.exception.ScoreboardRenderException;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class AsyncScoreboardRenderer implements ScoreboardRenderer {

    private final ScoreboardCacheManager cacheManager;

    @Override
    public CompletableFuture<Void> renderAsync(
            Player player,
            Scoreboard scoreboard,
            PlaceholderContext context,
            FastBoardAdapter adapter
    ) {
        if (adapter.isDeleted()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            PlaceholderContext finalContext = context != null ? context : PlaceholderContext.create();

            CompletableFuture<String> titleFuture = processStringAsync(
                    scoreboard.getTitle(),
                    player,
                    finalContext
            );

            CompletableFuture<List<String>> linesFuture = processLinesAsync(
                    scoreboard.getLines(),
                    player,
                    finalContext
            );

            return CompletableFuture.allOf(titleFuture, linesFuture)
                    .thenApply(v -> {
                        String processedTitle = titleFuture.join();
                        List<String> processedLines = linesFuture.join();

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

    @Override
    public void cleanup(Player player) {
        cacheManager.invalidatePlayer(player.getUniqueId());
    }

    private CompletableFuture<List<String>> processLinesAsync(
            List<ScoreboardLine> lines,
            Player player,
            PlaceholderContext context
    ) {
        if (lines == null || lines.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<String>> lineFutures = lines.stream()
                .map(line -> processLineAsync(line, player, context))
                .toList();

        return CompletableFuture.allOf(lineFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> lineFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }

    private CompletableFuture<String> processLineAsync(
            ScoreboardLine line,
            Player player,
            PlaceholderContext context
    ) {
        LineCacheKey cacheKey = LineCacheKey.of(player, line.getContent());

        String cached = cacheManager.getLine(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return processStringAsync(line.getContent(), player, context)
                .thenApply(processed -> {
                    cacheManager.cacheLine(cacheKey, processed);
                    return processed;
                });
    }

    private CompletableFuture<String> processStringAsync(
            String text,
            Player player,
            PlaceholderContext context
    ) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture("");
        }

        return Placeholders.processAsync(text, player, context)
                .thenApply(ColorAPI::parseToString);
    }
}
