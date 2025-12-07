package net.exylia.commons.v2.visual.config;

import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ActionBarConfig extends VisualConfig {
    private final String text;

    public ActionBarConfig(String text, boolean permanent, long updateInterval) {
        this.text = text;
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

        if (permanent && updateInterval < 1) {
            errors.add("Update interval must be >= 1 for permanent action bars");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
