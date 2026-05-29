package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.TitleConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleRenderer implements VisualRenderer<TitleConfig> {
    private static final TitleRenderer INSTANCE = new TitleRenderer();

    private TitleRenderer() {
    }

    public static TitleRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, TitleConfig config, PlaceholderContext context) {
        if (!player.isOnline()) return;

        Component titleComponent = CacheManager.getInstance()
                .processAndParse(config.getTitle(), player, context);

        Component subtitleComponent = CacheManager.getInstance()
                .processAndParse(config.getSubtitle(), player, context);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(config.getFadeIn() * 50L),
                Duration.ofMillis(config.getStay() * 50L),
                Duration.ofMillis(config.getFadeOut() * 50L)
        );

        Title title = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(title);
    }

    @Override
    public void cleanup(Player player, String visualId) {
        if (player.isOnline()) {
            player.clearTitle();
        }
    }
}
