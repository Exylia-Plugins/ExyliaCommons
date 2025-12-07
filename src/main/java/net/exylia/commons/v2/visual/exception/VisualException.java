package net.exylia.commons.v2.visual.exception;

public class VisualException extends RuntimeException {
    public VisualException(String message) {
        super(message);
    }

    public VisualException(String message, Throwable cause) {
        super(message, cause);
    }

    public VisualException(Throwable cause) {
        super(cause);
    }
}
