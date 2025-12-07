package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ActionBarRenderer implements VisualRenderer<ActionBarConfig> {
    private static final ActionBarRenderer INSTANCE = new ActionBarRenderer();

    private ActionBarRenderer() {
    }

    public static ActionBarRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, ActionBarConfig config, PlaceholderContext context) {
        return AsyncExecutor.getInstance()
                .supplyAsync(() -> {
                    Component component = CacheManager.getInstance()
                            .processAndParse(config.getText(), player, context);
                    return component;
                }, false)
                .thenAcceptAsync(component -> {
                    SchedulerManager.getInstance().runSync(() -> {
                        if (player.isOnline()) {
                            player.sendActionBar(component);
                        }
                    });
                }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
