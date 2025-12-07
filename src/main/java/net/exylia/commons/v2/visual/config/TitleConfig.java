package net.exylia.commons.v2.visual.config;

import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TitleConfig extends VisualConfig {
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public TitleConfig(
            String title,
            String subtitle,
            int fadeIn,
            int stay,
            int fadeOut,
            boolean permanent,
            long updateInterval
    ) {
        this.title = title != null ? title : "";
        this.subtitle = subtitle != null ? subtitle : "";
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
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

        if ((title == null || title.isBlank()) && (subtitle == null || subtitle.isBlank())) {
            errors.add("Title or subtitle must be provided");
        }

        if (fadeIn < 0) {
            errors.add("Fade in must be >= 0");
        }

        if (stay < 0) {
            errors.add("Stay must be >= 0");
        }

        if (fadeOut < 0) {
            errors.add("Fade out must be >= 0");
        }

        if (permanent && updateInterval < 1) {
            errors.add("Update interval must be >= 1 for permanent titles");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
