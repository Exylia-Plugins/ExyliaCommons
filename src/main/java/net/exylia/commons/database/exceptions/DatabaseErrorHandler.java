package net.exylia.commons.database.exceptions;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;
import java.util.logging.Level;

public class DatabaseErrorHandler {

    private final ExyliaPlugin plugin;

    public DatabaseErrorHandler(ExyliaPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleError(DatabaseException exception) {
         
        String detailedMessage = exception.getDetailedMessage();

        DebugUtils.logInternalError("Full Database Error Details:\n" + detailedMessage);
        plugin.getLogger().log(Level.SEVERE, "Root cause stack trace:", exception.getCause());
    }

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

    public void logWarning(String operation, String entityClass, String message) {
        String fullMessage = String.format("Database Warning - Operation: %s, Entity: %s, Message: %s",
                operation, entityClass, message);
        DebugUtils.logInternalWarn(fullMessage);
    }

    public void logRecovery(String operation, String entityClass, String recoveryAction) {
        String message = String.format("Database Recovery - Operation: %s, Entity: %s, Recovery: %s",
                operation, entityClass, recoveryAction);
        DebugUtils.logInternalInfo(message);
    }
}
