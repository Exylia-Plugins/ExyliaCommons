package net.exylia.commons.chat.input;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Result of input processing
 */
@Getter
public class InputResult {

    private final InputResultType type;
    private final Object value;
    private final Object message;

    private InputResult(InputResultType type, Object value, Object message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }

    /**
     * Input was successful and should complete
     */
    public static InputResult success(Object value) {
        return new InputResult(InputResultType.SUCCESS, value, null);
    }

    /**
     * Input was successful with a message
     */
    public static InputResult success(Object value, String message) {
        return new InputResult(InputResultType.SUCCESS, value, message);
    }

    /**
     * Input was successful with a Component message
     */
    public static InputResult success(Object value, Component message) {
        return new InputResult(InputResultType.SUCCESS, value, message);
    }

    /**
     * Input was invalid, continue waiting for input
     */
    public static InputResult invalid() {
        return new InputResult(InputResultType.INVALID, null, null);
    }

    /**
     * Input was invalid with error message
     */
    public static InputResult invalid(String message) {
        return new InputResult(InputResultType.INVALID, null, message);
    }

    /**
     * Input was invalid with error Component message
     */
    public static InputResult invalid(Component message) {
        return new InputResult(InputResultType.INVALID, null, message);
    }

    /**
     * Cancel the input process
     */
    public static InputResult cancel() {
        return new InputResult(InputResultType.CANCEL, null, null);
    }

    /**
     * Cancel with message
     */
    public static InputResult cancel(String message) {
        return new InputResult(InputResultType.CANCEL, null, message);
    }

    /**
     * Cancel with Component message
     */
    public static InputResult cancel(Component message) {
        return new InputResult(InputResultType.CANCEL, null, message);
    }

    /**
     * Get message as String (if it's a String)
     */
    public String getMessageAsString() {
        return message instanceof String ? (String) message : null;
    }

    /**
     * Get message as Component (if it's a Component)
     */
    public Component getMessageAsComponent() {
        return message instanceof Component ? (Component) message : null;
    }

    /**
     * Check if message is a String
     */
    public boolean hasStringMessage() {
        return message instanceof String;
    }

    /**
     * Check if message is a Component
     */
    public boolean hasComponentMessage() {
        return message instanceof Component;
    }

    public enum InputResultType {
        SUCCESS,    // Complete the input with value
        INVALID,    // Invalid input, keep waiting
        CANCEL      // Cancel the input process
    }
}