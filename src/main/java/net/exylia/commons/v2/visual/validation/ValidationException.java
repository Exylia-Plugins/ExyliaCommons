package net.exylia.commons.v2.visual.validation;

import net.exylia.commons.v2.visual.exception.VisualException;

import java.util.List;

public class ValidationException extends VisualException {
    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
