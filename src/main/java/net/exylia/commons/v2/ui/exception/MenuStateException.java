package net.exylia.commons.v2.ui.exception;

import net.exylia.commons.v2.ui.model.MenuState;

public class MenuStateException extends MenuException {

    public MenuStateException(MenuState expected, MenuState actual) {
        super(String.format("Invalid menu state. Expected: %s, Actual: %s", expected, actual));
    }

    public MenuStateException(String message) {
        super(message);
    }

    public MenuStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
