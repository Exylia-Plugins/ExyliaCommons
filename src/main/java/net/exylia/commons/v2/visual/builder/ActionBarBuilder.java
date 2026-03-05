package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.ActionBarConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ActionBarBuilder extends VisualBuilder<ActionBarConfig, ActionBarBuilder> {
    private String text;

    private ActionBarBuilder() {
    }

    public static ActionBarBuilder create() {
        return new ActionBarBuilder();
    }

    public ActionBarBuilder text(String text) {
        this.text = text;
        return this;
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (text == null || text.isBlank()) {
            errors.add("Text cannot be null or blank");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected ActionBarConfig buildInternal() {
        return ActionBarConfig.builder(text)
                .updateInterval(updateInterval)
                .build();
    }
}
