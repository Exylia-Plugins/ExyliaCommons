package net.exylia.commons.v2.license.result;

import lombok.Getter;

@Getter
public class LicenseValidationResult {
    private final boolean valid;
    private final String provider;
    private final String message;
    private final ErrorType errorType;
    private final String detailedReason;

    public LicenseValidationResult(boolean valid, String provider, String message, ErrorType errorType, String detailedReason) {
        this.valid = valid;
        this.provider = provider;
        this.message = message;
        this.errorType = errorType;
        this.detailedReason = detailedReason;
    }

    public static LicenseValidationResult success(String provider) {
        return new LicenseValidationResult(true, provider, "License validated successfully", ErrorType.NONE, null);
    }

    public static LicenseValidationResult failure(String provider, ErrorType errorType, String message, String detailedReason) {
        return new LicenseValidationResult(false, provider, message, errorType, detailedReason);
    }

    public enum ErrorType {
        NONE,
        MISSING_LICENSE_FILE,
        INVALID_LICENSE_KEY,
        LICENSE_EXPIRED,
        LICENSE_SUSPENDED,
        PRODUCT_MISMATCH,
        HWID_MISMATCH,
        IP_MISMATCH,
        CONNECTION_ERROR,
        SERVER_ERROR,
        SIGNATURE_INVALID,
        RATE_LIMITED,
        UNKNOWN
    }
}
