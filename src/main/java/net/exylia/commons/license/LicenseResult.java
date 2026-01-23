package net.exylia.commons.license;

@Deprecated
public class LicenseResult {
    private final boolean valid;
    private final String message;
    private final ErrorType errorType;

    public LicenseResult(boolean valid, String message, ErrorType errorType) {
        this.valid = valid;
        this.message = message;
        this.errorType = errorType;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        NONE,
        MISSING_LICENSE,
        INVALID_LICENSE,
        CONNECTION_ERROR,
        SERVER_ERROR,
        VALIDATION_IN_PROGRESS,
        UNEXPECTED_ERROR
    }
}
