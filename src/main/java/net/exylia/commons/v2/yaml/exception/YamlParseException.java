package net.exylia.commons.v2.yaml.exception;

public class YamlParseException extends YamlException {

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
