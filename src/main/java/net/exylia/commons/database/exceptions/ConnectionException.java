package net.exylia.commons.database.exceptions;

@Deprecated
public class ConnectionException extends DatabaseException {

    private final String connectionString;
    private final int retryCount;

    public ConnectionException(String adapterType, String connectionString, String message, Throwable cause) {
        super("Connection", "N/A", adapterType, message, cause);
        this.connectionString = connectionString;
        this.retryCount = 0;
    }

    public ConnectionException(String adapterType, String connectionString, String message, int retryCount, Throwable cause) {
        super("Connection", "N/A", adapterType, message, cause);
        this.connectionString = connectionString;
        this.retryCount = retryCount;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Database Connection Failed:\n");
        sb.append("  Adapter: ").append(getAdapterType()).append("\n");
        sb.append("  Connection String: ").append(maskConnectionString(connectionString)).append("\n");
        sb.append("  Retry Count: ").append(retryCount).append("\n");
        sb.append("  Message: ").append(getMessage()).append("\n");

        if (getCause() != null) {
            sb.append("  Root Cause: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append("\n");
        }

        return sb.toString();
    }

    private String maskConnectionString(String connectionString) {
        if (connectionString == null) return "Unknown";

        return connectionString.replaceAll("password=[^&;]+", "password=***")
                .replaceAll("pwd=[^&;]+", "pwd=***");
    }

    public String getConnectionString() { return connectionString; }
    public int getRetryCount() { return retryCount; }
}
