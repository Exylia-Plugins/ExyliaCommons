package net.exylia.commons.v2.formatter.core;

public class FormatterException extends RuntimeException {

    public FormatterException(String message) {
        super(message);
    }

    public FormatterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatterException(Throwable cause) {
        super(cause);
    }
}
