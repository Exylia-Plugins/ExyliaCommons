package net.exylia.commons.v2.visual.config;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.visual.validation.ValidationResult;

@Getter
@Setter
public abstract class VisualConfig {
    protected boolean enabled = true;
    protected long updateInterval = 20L;
    protected boolean permanent = false;

    public abstract ValidationResult validate();

    protected ValidationResult validateBase() {
        if (updateInterval < 1) {
            return ValidationResult.failure("Update interval must be >= 1");
        }
        return ValidationResult.success();
    }
}
