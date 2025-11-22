package net.exylia.commons.databaseV2.exception;

import net.exylia.commons.utils.DebugUtils;

public class ExceptionHandler {

    public static void handle(Throwable throwable, String context) {
        if (throwable instanceof DatabaseV2Exception) {
            if (throwable instanceof ConnectionException) {
                DebugUtils.logWarn("Connection error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof SerializationException) {
                DebugUtils.logError("Serialization error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof CacheException) {
                DebugUtils.logWarn("Cache error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof RepositoryException) {
                DebugUtils.logError("Repository error in " + context + ": " + throwable.getMessage());
            } else {
                DebugUtils.logError("Database error in " + context + ": " + throwable.getMessage());
            }
        } else {
            DebugUtils.logError("Unexpected error in " + context + ": " + throwable.getMessage());
        }
    }

    public static void handleSilent(Throwable throwable, String context) {
        if (throwable instanceof DatabaseV2Exception) {
            DebugUtils.logDebug("Database operation error in " + context + ": " + throwable.getMessage());
        }
    }
}
