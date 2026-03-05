package net.exylia.commons.v2.visual.config;

import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ActionBarConfig extends VisualConfig {

    private final String text;

    private ActionBarConfig(Builder builder) {
        this.text = builder.text;
        this.updateInterval = builder.updateInterval;
    }

    public static Builder builder(String text) {
        return new Builder(text);
    }

    public static class Builder {
        private final String text;
        private long updateInterval = 20L;

        private Builder(String text) {
            this.text = text;
        }

        public Builder updateInterval(long updateInterval) { this.updateInterval = updateInterval; return this; }

        public ActionBarConfig build() { return new ActionBarConfig(this); }
    }

    @Override
    public ValidationResult validate() {
        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) return baseResult;

        List<String> errors = new ArrayList<>();
        if (text == null || text.isBlank()) errors.add("Text cannot be null or blank");

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
