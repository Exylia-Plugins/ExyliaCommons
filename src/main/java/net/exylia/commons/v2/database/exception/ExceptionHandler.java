package net.exylia.commons.v2.database.exception;

import net.exylia.commons.utils.DebugUtils;

public class ExceptionHandler {

    public static void handle(Throwable throwable, String context) {
        if (throwable instanceof DatabaseV2Exception) {
            if (throwable instanceof ConnectionException) {
                DebugUtils.logInternalWarn("Connection error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof SerializationException) {
                DebugUtils.logInternalError("Serialization error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof CacheException) {
                DebugUtils.logInternalWarn("Cache error in " + context + ": " + throwable.getMessage());
            } else if (throwable instanceof RepositoryException) {
                DebugUtils.logInternalError("Repository error in " + context + ": " + throwable.getMessage());
            } else {
                DebugUtils.logInternalError("Database error in " + context + ": " + throwable.getMessage());
            }
        } else {
            DebugUtils.logInternalError("Unexpected error in " + context + ": " + throwable.getMessage());
        }
    }

    public static void handleSilent(Throwable throwable, String context) {
        if (throwable instanceof DatabaseV2Exception) {
            DebugUtils.logDebug("Database operation error in " + context + ": " + throwable.getMessage());
        }
    }
}
