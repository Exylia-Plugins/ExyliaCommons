package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.visual.validation.ValidationException;
import net.exylia.commons.v2.visual.validation.ValidationResult;

public abstract class VisualBuilder<C extends VisualConfig, B extends VisualBuilder<C, B>> {
    protected boolean enabled = true;
    protected long updateInterval = 20L;
    protected boolean permanent = false;

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B enabled(boolean enabled) {
        this.enabled = enabled;
        return self();
    }

    public B updateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
        return self();
    }

    public B permanent(boolean permanent) {
        this.permanent = permanent;
        return self();
    }

    public B permanent() {
        return permanent(true);
    }

    protected abstract ValidationResult validateInternal();

    protected abstract C buildInternal();

    public final ValidationResult validate() {
        return validateInternal();
    }

    public final C build() {
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new ValidationException(result.getErrors());
        }
        return buildInternal();
    }
}
