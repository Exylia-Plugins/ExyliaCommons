package net.exylia.commons.chat.input;

import lombok.Getter;
import net.kyori.adventure.text.Component;

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

    public static InputResult success(Object value) {
        return new InputResult(InputResultType.SUCCESS, value, null);
    }

    public static InputResult success(Object value, String message) {
        return new InputResult(InputResultType.SUCCESS, value, message);
    }

    public static InputResult success(Object value, Component message) {
        return new InputResult(InputResultType.SUCCESS, value, message);
    }

    public static InputResult invalid() {
        return new InputResult(InputResultType.INVALID, null, null);
    }

    public static InputResult invalid(String message) {
        return new InputResult(InputResultType.INVALID, null, message);
    }

    public static InputResult invalid(Component message) {
        return new InputResult(InputResultType.INVALID, null, message);
    }

    public static InputResult cancel() {
        return new InputResult(InputResultType.CANCEL, null, null);
    }

    public static InputResult cancel(String message) {
        return new InputResult(InputResultType.CANCEL, null, message);
    }

    public static InputResult cancel(Component message) {
        return new InputResult(InputResultType.CANCEL, null, message);
    }

    public static InputResult retry() {
        return new InputResult(InputResultType.RETRY, null, null);
    }

    public static InputResult retry(String message) {
        return new InputResult(InputResultType.RETRY, null, message);
    }

    public static InputResult retry(Component message) {
        return new InputResult(InputResultType.RETRY, null, message);
    }

    public String getMessageAsString() {
        return message instanceof String ? (String) message : null;
    }

    public Component getMessageAsComponent() {
        return message instanceof Component ? (Component) message : null;
    }

    public boolean hasStringMessage() {
        return message instanceof String;
    }

    public boolean hasComponentMessage() {
        return message instanceof Component;
    }

    public enum InputResultType {
        SUCCESS,     
        INVALID,     
        CANCEL,      
        RETRY        
    }
}
