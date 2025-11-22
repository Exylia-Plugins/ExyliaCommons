package net.exylia.commons.placeholdersV2.exception;

public class PlaceholderException extends RuntimeException {
    public PlaceholderException(String message) {
        super(message);
    }

    public PlaceholderException(String message, Throwable cause) {
        super(message, cause);
    }
}
