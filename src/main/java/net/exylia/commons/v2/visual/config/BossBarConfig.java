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

    public BossBarConfig(
            String text,
            BossBar.Color color,
            BossBar.Overlay style,
            double progress,
            boolean permanent,
            long updateInterval
    ) {
        this.text = text;
        this.color = color != null ? color : BossBar.Color.BLUE;
        this.style = style != null ? style : BossBar.Overlay.PROGRESS;
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        this.permanent = permanent;
        this.updateInterval = updateInterval;
    }

    @Override
    public ValidationResult validate() {
        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) {
            return baseResult;
        }

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
}
