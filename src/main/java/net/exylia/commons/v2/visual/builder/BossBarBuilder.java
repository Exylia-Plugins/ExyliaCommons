package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import net.kyori.adventure.bossbar.BossBar;

import java.util.ArrayList;
import java.util.List;

public class BossBarBuilder extends VisualBuilder<BossBarConfig, BossBarBuilder> {
    private String text;
    private BossBar.Color color = BossBar.Color.BLUE;
    private BossBar.Overlay style = BossBar.Overlay.PROGRESS;
    private double progress = 1.0;

    private BossBarBuilder() {
    }

    public static BossBarBuilder create() {
        return new BossBarBuilder();
    }

    public BossBarBuilder text(String text) {
        this.text = text;
        return this;
    }

    public BossBarBuilder color(BossBar.Color color) {
        this.color = color;
        return this;
    }

    public BossBarBuilder style(BossBar.Overlay style) {
        this.style = style;
        return this;
    }

    public BossBarBuilder progress(double progress) {
        this.progress = progress;
        return this;
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (text == null || text.isBlank()) {
            errors.add("Text cannot be null or blank");
        }

        if (progress < 0.0 || progress > 1.0) {
            errors.add("Progress must be between 0.0 and 1.0");
        }

        if (permanent && updateInterval < 1) {
            errors.add("Update interval must be >= 1 for permanent boss bars");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected BossBarConfig buildInternal() {
        BossBarConfig config = BossBarConfig.builder(text)
                .color(color)
                .style(style)
                .progress(progress)
                .permanent(permanent)
                .updateInterval(updateInterval)
                .build();
        config.setEnabled(enabled);
        return config;
    }
}
