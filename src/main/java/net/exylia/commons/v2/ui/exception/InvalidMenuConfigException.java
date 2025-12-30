package net.exylia.commons.v2.ui.exception;

public class InvalidMenuConfigException extends MenuException {

    public InvalidMenuConfigException(String message) {
        super("Invalid menu configuration: " + message);
    }

    public InvalidMenuConfigException(String message, Throwable cause) {
        super("Invalid menu configuration: " + message, cause);
    }
}
