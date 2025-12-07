package net.exylia.commons.v2.visual.component;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.TitleAPI;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import net.exylia.commons.v2.visual.config.TitleConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TitleComponent implements VisualComponent<TitleConfig> {
    private static final TitleComponent INSTANCE = new TitleComponent();

    private TitleComponent() {}

    public static TitleComponent getInstance() {
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

        TitleConfig config = buildConfig(section);
        return TitleAPI.send(player, config, context);
    }

    @Override
    public CompletableFuture<String> send(Player player, ConfigurationSection section, Consumer<PlaceholderContext> contextBuilder) {
        PlaceholderContext context = PlaceholderContext.create().withPlayer(player);
        contextBuilder.accept(context);
        return send(player, section, context);
    }

    public CompletableFuture<String> sendPermanent(Player player, ConfigurationSection section) {
        return sendPermanent(player, section, PlaceholderContext.create());
    }

    public CompletableFuture<String> sendPermanent(Player player, ConfigurationSection section, PlaceholderContext context) {
        if (!isEnabled(section)) {
            return CompletableFuture.completedFuture(null);
        }

        TitleConfig config = buildConfig(section);
        return TitleAPI.sendPermanent(player, config, context);
    }

    public CompletableFuture<String> sendPermanent(
            Player player,
            ConfigurationSection section,
            Consumer<PlaceholderContext> contextBuilder
    ) {
        PlaceholderContext context = PlaceholderContext.create().withPlayer(player);
        contextBuilder.accept(context);
        return sendPermanent(player, section, context);
    }

    public CompletableFuture<String> countdown(Player player, ConfigurationSection section, Number duration) {
        return countdown(player, section, duration, PlaceholderContext.create());
    }

    public CompletableFuture<String> countdown(
            Player player,
            ConfigurationSection section,
            Number duration,
            PlaceholderContext context
    ) {
        if (!isEnabled(section)) {
            return CompletableFuture.completedFuture(null);
        }

        int durationSeconds = duration.intValue();
        TitleConfig config = buildConfig(section);
        return TitleAPI.countdown(player, durationSeconds, config, context);
    }

    public CompletableFuture<String> countdown(
            Player player,
            ConfigurationSection section,
            Number duration,
            Consumer<PlaceholderContext> contextBuilder
    ) {
        PlaceholderContext context = PlaceholderContext.create().withPlayer(player);
        contextBuilder.accept(context);
        return countdown(player, section, duration, context);
    }

    public CountdownTitleComponentBuilder countdownBuilder(
            Player player,
            ConfigurationSection section,
            Number duration
    ) {
        TitleConfig config = buildConfig(section);
        return new CountdownTitleComponentBuilder(player, duration.intValue(), config);
    }

    public static class CountdownTitleComponentBuilder {
        private final Player player;
        private final int durationSeconds;
        private final TitleConfig config;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownTitleComponentBuilder(Player player, int durationSeconds, TitleConfig config) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            this.config = config;
        }

        public CountdownTitleComponentBuilder context(PlaceholderContext context) {
            this.context = context;
            return this;
        }

        public CountdownTitleComponentBuilder context(Consumer<PlaceholderContext> contextBuilder) {
            PlaceholderContext ctx = PlaceholderContext.create().withPlayer(player);
            contextBuilder.accept(ctx);
            this.context = ctx;
            return this;
        }

        public CountdownTitleComponentBuilder onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownTitleComponentBuilder onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            return TitleAPI.countdownBuilder(player, durationSeconds)
                    .title(config.getTitle())
                    .subtitle(config.getSubtitle())
                    .times(config.getFadeIn(), config.getStay(), config.getFadeOut())
                    .context(context)
                    .onComplete(onComplete)
                    .onCancel(onCancel)
                    .start();
        }
    }

    private TitleConfig buildConfig(ConfigurationSection section) {
        String title = section.getString("title", "");
        String subtitle = section.getString("subtitle", "");
        int fadeIn = section.getInt("fadeIn", 10);
        int stay = section.getInt("stay", 70);
        int fadeOut = section.getInt("fadeOut", 20);
        boolean permanent = section.getBoolean("permanent", false);
        long updateInterval = section.getLong("update-interval", 20L);

        return TitleBuilder.create()
                .title(title)
                .subtitle(subtitle)
                .times(fadeIn, stay, fadeOut)
                .permanent(permanent)
                .updateInterval(updateInterval)
                .build();
    }
}
