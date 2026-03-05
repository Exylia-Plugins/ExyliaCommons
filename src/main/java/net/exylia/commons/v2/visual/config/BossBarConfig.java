package net.exylia.commons.v2.visual.config;

import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import net.kyori.adventure.bossbar.BossBar;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BossBarConfig extends VisualConfig {

    private final String text;
    private final BossBar.Color color;
    private final BossBar.Overlay style;
    private final double progress;

    private BossBarConfig(Builder builder) {
        this.text = builder.text;
        this.color = builder.color;
        this.style = builder.style;
        this.progress = Math.max(0.0, Math.min(1.0, builder.progress));
        this.updateInterval = builder.updateInterval;
    }

    public static Builder builder(String text) {
        return new Builder(text);
    }

    public static class Builder {
        private final String text;
        private BossBar.Color color = BossBar.Color.BLUE;
        private BossBar.Overlay style = BossBar.Overlay.PROGRESS;
        private double progress = 1.0;
        private long updateInterval = 20L;

        private Builder(String text) {
            this.text = text;
        }

        public Builder color(BossBar.Color color) { this.color = color; return this; }
        public Builder style(BossBar.Overlay style) { this.style = style; return this; }
        public Builder progress(double progress) { this.progress = progress; return this; }
        public Builder updateInterval(long updateInterval) { this.updateInterval = updateInterval; return this; }

        public BossBarConfig build() { return new BossBarConfig(this); }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) return baseResult;

        List<String> errors = new ArrayList<>();
        if (text == null || text.isBlank()) errors.add("Text cannot be null or blank");
        if (progress < 0.0 || progress > 1.0) errors.add("Progress must be between 0.0 and 1.0");

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
