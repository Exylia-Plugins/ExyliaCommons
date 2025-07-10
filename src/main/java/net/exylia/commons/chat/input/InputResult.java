package net.exylia.commons.chat.input;

import lombok.Getter;

/**
 * Result of input processing
 */
@Getter
public class InputResult {

    private final InputResultType type;
    private final Object value;
    private final String message;

    private InputResult(InputResultType type, Object value, String message) {
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

    public enum InputResultType {
        SUCCESS,    // Complete the input with value
        INVALID,    // Invalid input, keep waiting
        CANCEL      // Cancel the input process
    }
}