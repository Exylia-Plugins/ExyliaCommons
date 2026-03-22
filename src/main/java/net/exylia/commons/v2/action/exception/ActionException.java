package net.exylia.commons.v2.action.exception;

public class ActionException extends RuntimeException {
    private final boolean expected;

    public ActionException(String message) {
        super(message);
        this.expected = false;
    }

    public ActionException(String message, boolean expected) {
        super(message);
        this.expected = expected;
    }

    public ActionException(String message, Throwable cause) {
        super(message, cause);
        this.expected = false;
    }

    public boolean isExpected() {
        return expected;
    }

    public static class ActionNotFoundException extends ActionException {
        public ActionNotFoundException(String actionId) {
            super("Action not found: " + actionId);
        }
    }

    public static class ActionAlreadyExistsException extends ActionException {
        public ActionAlreadyExistsException(String actionId) {
            super("Action already exists: " + actionId);
        }
    }

    public static class ActionExecutionException extends ActionException {
        public ActionExecutionException(String message) {
            super(message);
        }

        public ActionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ActionCooldownException extends ActionException {
        private final long remainingMillis;

        public ActionCooldownException(String actionId, long remainingMillis) {
            super("Action '" + actionId + "' is on cooldown for " + remainingMillis + "ms", true);
            this.remainingMillis = remainingMillis;
        }

        public long getRemainingMillis() {
            return remainingMillis;
        }
    }

    public static class ActionRateLimitException extends ActionException {
        public ActionRateLimitException(String actionId) {
            super("Rate limit exceeded for action: " + actionId, true);
        }

        public ActionRateLimitException(String actionId, long retryAfterMillis) {
            super("Rate limit exceeded for action: " + actionId + ". Retry after " + retryAfterMillis + "ms", true);
        }
    }

    public static class ActionPermissionException extends ActionException {
        private final String permission;

        public ActionPermissionException(String permission) {
            super("Missing permission: " + permission, true);
            this.permission = permission;
        }

        public String getPermission() {
            return permission;
        }
    }

    public static class ActionParseException extends ActionException {
        public ActionParseException(String message) {
            super("Failed to parse action: " + message);
        }

        public ActionParseException(String message, Throwable cause) {
            super("Failed to parse action: " + message, cause);
        }
    }

    public static class ActionValidationException extends ActionException {
        public ActionValidationException(String message) {
            super("Action validation failed: " + message);
        }
    }
}
