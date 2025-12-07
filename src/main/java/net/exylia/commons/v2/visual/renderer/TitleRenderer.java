package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TitleRenderer implements VisualRenderer<TitleConfig> {
    private static final TitleRenderer INSTANCE = new TitleRenderer();

    private TitleRenderer() {
    }

    public static TitleRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, TitleConfig config, PlaceholderContext context) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> {
                    Component titleComponent = CacheManager.getInstance()
                            .processAndParse(config.getTitle(), player, context);

                    Component subtitleComponent = CacheManager.getInstance()
                            .processAndParse(config.getSubtitle(), player, context);

                    return new TitleData(titleComponent, subtitleComponent);
                }, false)
                .thenAcceptAsync(data -> {
                    SchedulerManager.getInstance().runSync(() -> {
                        if (!player.isOnline()) return;

                        Title.Times times = Title.Times.times(
                                Duration.ofMillis(config.getFadeIn() * 50L),
                                Duration.ofMillis(config.getStay() * 50L),
                                Duration.ofMillis(config.getFadeOut() * 50L)
                        );

                        Title title = Title.title(data.title, data.subtitle, times);
                        player.showTitle(title);
                    });
                }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }

    private record TitleData(Component title, Component subtitle) {
    }
}
