package net.exylia.commons.v2.database.exception;

import net.exylia.commons.utils.DebugUtils;

public class ExceptionHandler {

    public static void handle(Throwable throwable, String context) {
        if (throwable instanceof DatabaseException) {
            switch (throwable) {
                case ConnectionException connectionException ->
                        DebugUtils.logInternalWarn("Connection error in " + context + ": " + throwable.getMessage());
                case SerializationException serializationException ->
                        DebugUtils.logInternalError("Serialization error in " + context + ": " + throwable.getMessage());
                case CacheException cacheException ->
                        DebugUtils.logInternalWarn("Cache error in " + context + ": " + throwable.getMessage());
                case RepositoryException repositoryException ->
                        DebugUtils.logInternalError("Repository error in " + context + ": " + throwable.getMessage());
                default -> DebugUtils.logInternalError("Database error in " + context + ": " + throwable.getMessage());
            }
        } else {
            DebugUtils.logInternalError("Unexpected error in " + context + ": " + throwable.getMessage());
        }
    }

    public static void handleSilent(Throwable throwable, String context) {
        if (throwable instanceof DatabaseException) {
            DebugUtils.logDebug("Database operation error in " + context + ": " + throwable.getMessage());
        }
    }
}
