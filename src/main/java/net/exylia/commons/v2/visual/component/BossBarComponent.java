package net.exylia.commons.v2.visual.component;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.BossBarAPI;
import net.exylia.commons.v2.visual.builder.BossBarBuilder;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BossBarComponent implements VisualComponent<BossBarConfig> {
    private static final BossBarComponent INSTANCE = new BossBarComponent();

    private BossBarComponent() {}

    public static BossBarComponent getInstance() {
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

        BossBarConfig config = buildConfig(section);
        return BossBarAPI.send(player, config, context);
    }

    @Override
    public CompletableFuture<String> send(Player player, ConfigurationSection section, Consumer<PlaceholderContext> contextBuilder) {
        PlaceholderContext context = PlaceholderContext.create().withPlayer(player);
        contextBuilder.accept(context);
        return send(player, section, context);
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
        BossBarConfig config = buildConfig(section);
        return BossBarAPI.countdown(player, durationSeconds, config, context);
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

    public CountdownBossBarComponentBuilder countdownBuilder(
            Player player,
            ConfigurationSection section,
            Number duration
    ) {
        BossBarConfig config = buildConfig(section);
        return new CountdownBossBarComponentBuilder(player, duration.intValue(), config);
    }

    public static class CountdownBossBarComponentBuilder {
        private final Player player;
        private final int durationSeconds;
        private final BossBarConfig config;
        private PlaceholderContext context = PlaceholderContext.create();
        private Runnable onComplete;
        private Runnable onCancel;

        private CountdownBossBarComponentBuilder(Player player, int durationSeconds, BossBarConfig config) {
            this.player = player;
            this.durationSeconds = durationSeconds;
            this.config = config;
        }

        public CountdownBossBarComponentBuilder context(PlaceholderContext context) {
            this.context = context;
            return this;
        }

        public CountdownBossBarComponentBuilder context(Consumer<PlaceholderContext> contextBuilder) {
            PlaceholderContext ctx = PlaceholderContext.create().withPlayer(player);
            contextBuilder.accept(ctx);
            this.context = ctx;
            return this;
        }

        public CountdownBossBarComponentBuilder onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        public CountdownBossBarComponentBuilder onCancel(Runnable callback) {
            this.onCancel = callback;
            return this;
        }

        public CompletableFuture<String> start() {
            return BossBarAPI.countdownBuilder(player, durationSeconds)
                    .text(config.getText())
                    .color(config.getColor())
                    .style(config.getStyle())
                    .context(context)
                    .onComplete(onComplete)
                    .onCancel(onCancel)
                    .start();
        }
    }

    private BossBarConfig buildConfig(ConfigurationSection section) {
        String text = section.getString("text", "");
        String colorStr = section.getString("color", "BLUE").toUpperCase();
        String styleStr = section.getString("style", "PROGRESS").toUpperCase();
        double progress = section.getDouble("progress", 1.0);
        boolean permanent = section.getBoolean("permanent", false);
        long updateInterval = section.getLong("update-interval", 20L);

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.BLUE;
        }

        BossBar.Overlay style;
        try {
            style = BossBar.Overlay.valueOf(styleStr);
        } catch (IllegalArgumentException e) {
            style = BossBar.Overlay.PROGRESS;
        }

        return BossBarBuilder.create()
                .text(text)
                .color(color)
                .style(style)
                .progress(progress)
                .permanent(permanent)
                .updateInterval(updateInterval)
                .build();
    }
}
