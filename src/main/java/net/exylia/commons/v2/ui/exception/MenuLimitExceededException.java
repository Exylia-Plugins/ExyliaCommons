package net.exylia.commons.v2.ui.exception;

import net.exylia.commons.v2.ui.model.MenuType;

public class MenuLimitExceededException extends MenuException {
    public MenuLimitExceededException(MenuType type, int limit) {
        super(String.format("Menu limit exceeded for type %s. Limit: %d", type, limit));
    }

    public MenuLimitExceededException(String message) {
        super(message);
    }
}
