package net.exylia.commons.v2.visual.component;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.ActionBarAPI;
import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.config.ActionBarConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ActionBarComponent implements VisualComponent<ActionBarConfig> {
    private static final ActionBarComponent INSTANCE = new ActionBarComponent();

    private ActionBarComponent() {}

    public static ActionBarComponent getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<String> send(Player player, ConfigurationSection section) {
        return send(player, section, PlaceholderContext.create());
    }

    @Override
    public CompletableFuture<String> send(Player player, ConfigurationSection section, PlaceholderContext context) {
        if (!isEnabled(section)) {
            return CompletableFuture.completedFuture(null);
        }

        ActionBarConfig config = buildConfig(section);
        return ActionBarAPI.send(player, config, context);
    }

    @Override
    public CompletableFuture<String> send(Player player, ConfigurationSection section, Consumer<PlaceholderContext> contextBuilder) {
        PlaceholderContext context = PlaceholderContext.create().withPlayer(player);
        contextBuilder.accept(context);
        return send(player, section, context);
    }

    private ActionBarConfig buildConfig(ConfigurationSection section) {
        String text = section.getString("text", "");
        long updateInterval = section.getLong("update-interval", 20L);

        return ActionBarBuilder.create()
                .text(text)
                .updateInterval(updateInterval)
                .build();
    }
}
