package net.exylia.commons.placeholdersV2.exception;

public class PlaceholderResolutionException extends PlaceholderException {
    private final String placeholderName;

    public PlaceholderResolutionException(String placeholderName, String message) {
        super("Error resolving placeholder '" + placeholderName + "': " + message);
        this.placeholderName = placeholderName;
    }

    public PlaceholderResolutionException(String placeholderName, String message, Throwable cause) {
        super("Error resolving placeholder '" + placeholderName + "': " + message, cause);
        this.placeholderName = placeholderName;
    }

    public String getPlaceholderName() {
        return placeholderName;
    }
}
