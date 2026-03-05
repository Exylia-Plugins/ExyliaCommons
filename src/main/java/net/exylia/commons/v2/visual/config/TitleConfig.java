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

    private TitleConfig(Builder builder) {
        this.title = builder.title != null ? builder.title : "";
        this.subtitle = builder.subtitle != null ? builder.subtitle : "";
        this.fadeIn = builder.fadeIn;
        this.stay = builder.stay;
        this.fadeOut = builder.fadeOut;
        this.updateInterval = builder.updateInterval;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "";
        private String subtitle = "";
        private int fadeIn = 10;
        private int stay = 70;
        private int fadeOut = 20;
        private long updateInterval = 20L;

        private Builder() {}

        public Builder title(String title) { this.title = title; return this; }
        public Builder subtitle(String subtitle) { this.subtitle = subtitle; return this; }
        public Builder fadeIn(int fadeIn) { this.fadeIn = fadeIn; return this; }
        public Builder stay(int stay) { this.stay = stay; return this; }
        public Builder fadeOut(int fadeOut) { this.fadeOut = fadeOut; return this; }
        public Builder updateInterval(long updateInterval) { this.updateInterval = updateInterval; return this; }

        public TitleConfig build() { return new TitleConfig(this); }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) return baseResult;

        List<String> errors = new ArrayList<>();
        if ((title == null || title.isBlank()) && (subtitle == null || subtitle.isBlank()))
            errors.add("Title or subtitle must be provided");
        if (fadeIn < 0) errors.add("Fade in must be >= 0");
        if (stay < 0) errors.add("Stay must be >= 0");
        if (fadeOut < 0) errors.add("Fade out must be >= 0");

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
