package net.exylia.commons.v2.ui.exception;

import net.exylia.commons.v2.ui.model.MenuState;

public class InvalidMenuStateException extends MenuException {
    public InvalidMenuStateException(MenuState current, MenuState expected) {
        super(String.format("Invalid menu state. Current: %s, Expected: %s", current, expected));
    }

    public InvalidMenuStateException(String message) {
        super(message);
    }
}
