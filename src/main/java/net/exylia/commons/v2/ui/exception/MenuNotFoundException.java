package net.exylia.commons.v2.ui.exception;

public class MenuNotFoundException extends MenuException {
    public MenuNotFoundException(String menuId) {
        super("Menu not found: " + menuId);
    }

    public MenuNotFoundException(String menuId, Throwable cause) {
        super("Menu not found: " + menuId, cause);
    }
}
