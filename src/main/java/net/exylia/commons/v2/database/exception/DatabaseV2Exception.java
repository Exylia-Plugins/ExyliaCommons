package net.exylia.commons.v2.database.exception;

public class DatabaseV2Exception extends RuntimeException {

    public DatabaseV2Exception(String message) {
        super(message);
    }

    public DatabaseV2Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseV2Exception(Throwable cause) {
        super(cause);
    }
}
