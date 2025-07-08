package net.exylia.commons.item.exceptions;

/**
 * Excepción base para errores relacionados con items
 */
public class ItemException extends RuntimeException {

    public ItemException(String message) {
        super(message);
    }

    public ItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemException(Throwable cause) {
        super(cause);
    }
}