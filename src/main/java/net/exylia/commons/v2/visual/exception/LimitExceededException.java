package net.exylia.commons.v2.visual.exception;

public class LimitExceededException extends VisualException {
    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
