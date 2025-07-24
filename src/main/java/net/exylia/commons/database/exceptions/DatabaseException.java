package net.exylia.commons.database.exceptions;

/**
 * Custom exception class for database operations with enhanced error details
 */
public class DatabaseException extends RuntimeException {

    private final String operation;
    private final String entityClass;
    private final String adapterType;
    private final String errorCode;
    private final Object[] parameters;

    public DatabaseException(String operation, String entityClass, String adapterType, String message) {
        super(message);
        this.operation = operation;
        this.entityClass = entityClass;
        this.adapterType = adapterType;
        this.errorCode = null;
        this.parameters = null;
    }

    public DatabaseException(String operation, String entityClass, String adapterType, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.entityClass = entityClass;
        this.adapterType = adapterType;
        this.errorCode = null;
        this.parameters = null;
    }

    public DatabaseException(String operation, String entityClass, String adapterType, String message, String errorCode, Object[] parameters, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.entityClass = entityClass;
        this.adapterType = adapterType;
        this.errorCode = errorCode;
        this.parameters = parameters;
    }

    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Database Operation Failed:\n");
        sb.append("  Operation: ").append(operation != null ? operation : "Unknown").append("\n");
        sb.append("  Entity: ").append(entityClass != null ? entityClass : "Unknown").append("\n");
        sb.append("  Adapter: ").append(adapterType != null ? adapterType : "Unknown").append("\n");

        if (errorCode != null) {
            sb.append("  Error Code: ").append(errorCode).append("\n");
        }

        if (parameters != null && parameters.length > 0) {
            sb.append("  Parameters: [");
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters[i] != null ? parameters[i].toString() : "null");
            }
            sb.append("]\n");
        }

        sb.append("  Message: ").append(getMessage()).append("\n");

        if (getCause() != null) {
            sb.append("  Root Cause: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append("\n");
        }

        return sb.toString();
    }

    // Getters
    public String getOperation() { return operation; }
    public String getEntityClass() { return entityClass; }
    public String getAdapterType() { return adapterType; }
    public String getErrorCode() { return errorCode; }
    public Object[] getParameters() { return parameters; }
}