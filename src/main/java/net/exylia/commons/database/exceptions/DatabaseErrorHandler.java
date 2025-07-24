package net.exylia.commons.database.exceptions;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;
import java.util.logging.Level;

/**
 * Centralized error handler for database operations
 */
public class DatabaseErrorHandler {

    private final ExyliaPlugin plugin;
    private final boolean debugMode;

    public DatabaseErrorHandler(ExyliaPlugin plugin, boolean debugMode) {
        this.plugin = plugin;
        this.debugMode = debugMode;
    }

    /**
     * Handle and log database exceptions with detailed information
     */
    public void handleError(DatabaseException exception) {
        // Always log the detailed message
        String detailedMessage = exception.getDetailedMessage();

        if (debugMode) {
            // In debug mode, log full details including stack trace
            plugin.getLogger().log(Level.SEVERE, detailedMessage, exception);
            DebugUtils.logInternalError("Full Database Error Details:\n" + detailedMessage);

            // Also log the full stack trace
            if (exception.getCause() != null) {
                plugin.getLogger().log(Level.SEVERE, "Root cause stack trace:", exception.getCause());
            }
        } else {
            // In production, log essential information without overwhelming the console
            plugin.getLogger().severe(detailedMessage);
        }
    }

    /**
     * Handle generic exceptions and convert them to DatabaseException if needed
     */
    public void handleGenericError(String operation, String entityClass, String adapterType, Exception exception) {
        DatabaseException dbException;

        if (exception instanceof DatabaseException) {
            dbException = (DatabaseException) exception;
        } else {
            dbException = new DatabaseException(operation, entityClass, adapterType,
                    "Unexpected error during database operation", exception);
        }

        handleError(dbException);
    }

    /**
     * Create a user-friendly error message
     */
    public String createUserFriendlyMessage(DatabaseException exception) {
        StringBuilder sb = new StringBuilder();
        sb.append("Database error in ").append(exception.getOperation() != null ? exception.getOperation().toLowerCase() : "operation");

        if (exception.getEntityClass() != null && !exception.getEntityClass().equals("Unknown")) {
            sb.append(" for ").append(exception.getEntityClass());
        }

        sb.append(": ").append(exception.getMessage());

        if (debugMode && exception.getCause() != null) {
            sb.append(" (Caused by: ").append(exception.getCause().getMessage()).append(")");
        }

        return sb.toString();
    }

    /**
     * Log a warning for recoverable errors
     */
    public void logWarning(String operation, String entityClass, String message) {
        String fullMessage = String.format("Database Warning - Operation: %s, Entity: %s, Message: %s",
                operation, entityClass, message);
        plugin.getLogger().warning(fullMessage);

        if (debugMode) {
            DebugUtils.logInternalWarn(fullMessage);
        }
    }

    /**
     * Log successful recovery from an error
     */
    public void logRecovery(String operation, String entityClass, String recoveryAction) {
        String message = String.format("Database Recovery - Operation: %s, Entity: %s, Recovery: %s",
                operation, entityClass, recoveryAction);
        plugin.getLogger().info(message);

        if (debugMode) {
            DebugUtils.logInternalInfo(message);
        }
    }
}