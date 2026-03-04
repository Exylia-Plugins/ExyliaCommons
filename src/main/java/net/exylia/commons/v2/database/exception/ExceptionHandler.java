package net.exylia.commons.v2.database.exception;

import net.exylia.commons.v2.debug.api.DebugAPI;

public class ExceptionHandler {

    public static void handle(Throwable throwable, String context) {
        if (throwable instanceof DatabaseException) {
            switch (throwable) {
                case ConnectionException connectionException ->
                        DebugAPI.logLibWarn("Connection error in " + context + ": " + throwable.getMessage());
                case SerializationException serializationException ->
                        DebugAPI.logLibError("Serialization error in " + context + ": " + throwable.getMessage());
                case CacheException cacheException ->
                        DebugAPI.logLibWarn("Cache error in " + context + ": " + throwable.getMessage());
                case RepositoryException repositoryException ->
                        DebugAPI.logLibError("Repository error in " + context + ": " + throwable.getMessage());
                default -> DebugAPI.logLibError("Database error in " + context + ": " + throwable.getMessage());
            }
        } else {
            DebugAPI.logLibError("Unexpected error in " + context + ": " + throwable.getMessage());
        }
    }

    public static void handleSilent(Throwable throwable, String context) {
        if (throwable instanceof DatabaseException) {
            DebugAPI.logLibDebug("Database operation error in " + context + ": " + throwable.getMessage());
        }
    }
}
